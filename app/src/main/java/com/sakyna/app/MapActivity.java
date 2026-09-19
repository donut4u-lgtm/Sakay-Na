
package com.sakyna.app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
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
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
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

        if (mode == null) {
            mode = getIntent().getStringExtra("map_mode");
        }

        rideId = getIntent().getStringExtra("ride_id");

        if (rideId == null) {
            rideId = getIntent().getStringExtra("rideId");
        }

        if (rideId == null) {
            rideId = "";
        }

        buildScreen();

        /*
         * Start location immediately.
         * The map will automatically move when GPS returns.
         */
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

        /*
         * SEARCH AREA IS FIXED ABOVE THE MAP.
         */
        LinearLayout searchRow =
                new LinearLayout(this);

        searchRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        searchRow.setGravity(
                Gravity.CENTER_VERTICAL
        );

        searchInput =
                new EditText(this);

        searchInput.setHint(
                "Search place, street, barangay..."
        );

        searchInput.setSingleLine(true);
        searchInput.setTextSize(16);
        searchInput.setTextColor(Color.BLACK);
        searchInput.setHintTextColor(Color.GRAY);
        searchInput.setBackgroundColor(
                Color.rgb(240, 240, 240)
        );

        searchRow.addView(
                searchInput,
                new LinearLayout.LayoutParams(
                        0,
                        65,
                        1
                )
        );

        Button searchButton =
                new Button(this);

        searchButton.setText(
                "SEARCH"
        );

        searchButton.setTextColor(
                Color.WHITE
        );

        searchButton.setBackgroundColor(
                Color.rgb(0, 120, 215)
        );

        searchButton.setOnClickListener(
                v -> searchPlace()
        );

        searchRow.addView(
                searchButton,
                new LinearLayout.LayoutParams(
                        145,
                        65
                )
        );

        root.addView(
                searchRow,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        75
                )
        );

        statusText =
                new TextView(this);

        statusText.setText(
                "🗺️ Detecting your current location..."
        );

        statusText.setTextSize(14);
        statusText.setTextColor(
                Color.DKGRAY
        );

        statusText.setPadding(
                12,
                5,
                12,
                5
        );

        root.addView(
                statusText,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        45
                )
        );

        mapView =
                new MapView(this);

        mapView.setTileSource(
                TileSourceFactory.MAPNIK
        );

        mapView.setMultiTouchControls(
                true
        );

        mapView.getController()
                .setZoom(15.0);

        mapView.getController()
                .setCenter(
                        new GeoPoint(
                                currentLat,
                                currentLng
                        )
                );

        /*
         * MAP GETS THE REMAINING SPACE.
         * SEARCH BOX REMAINS VISIBLE.
         */
        root.addView(
                mapView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        Button myLocationButton =
                new Button(this);

        myLocationButton.setText(
                "📍 CENTER ON MY LOCATION"
        );

        myLocationButton.setOnClickListener(
                v -> showCurrentLocation()
        );

        root.addView(
                myLocationButton,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        60
                )
        );

        TextView resultTitle =
                new TextView(this);

        resultTitle.setText(
                "SEARCH RESULTS"
        );

        resultTitle.setTextSize(17);
        resultTitle.setTextColor(Color.BLACK);
        resultTitle.setPadding(
                12,
                5,
                12,
                5
        );

        root.addView(resultTitle);

        ScrollView resultScroll =
                new ScrollView(this);

        resultsContainer =
                new LinearLayout(this);

        resultsContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        resultsContainer.setPadding(
                12,
                0,
                12,
                0
        );

        resultScroll.addView(
                resultsContainer
        );

        root.addView(
                resultScroll,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        210
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

        root.addView(
                confirmButton,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        60
                )
        );

        setContentView(root);
    }

    private void startLocation() {

        locationManager =
                (LocationManager)
                        getSystemService(
                                Context.LOCATION_SERVICE
                        );

        if (locationManager == null) {
            return;
        }

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

                        handler.post(() -> {

                            updateCurrentMarker();

                            statusText.setText(
                                    String.format(
                                            Locale.US,
                                            "📍 Current location: %.6f, %.6f",
                                            currentLat,
                                            currentLng
                                    )
                            );

                            /*
                             * Automatically center the map
                             * on the first real GPS location.
                             */
                            if (currentMarker == null) {
                                showCurrentLocation();
                            }
                        });
                    }
                };

        try {

            Location lastGps =
                    locationManager.getLastKnownLocation(
                            LocationManager.GPS_PROVIDER
                    );

            if (lastGps != null) {

                currentLat =
                        lastGps.getLatitude();

                currentLng =
                        lastGps.getLongitude();

                updateCurrentMarker();

                mapView.getController()
                        .setCenter(
                                new GeoPoint(
                                        currentLat,
                                        currentLng
                                )
                        );
            }

            Location lastNetwork =
                    locationManager.getLastKnownLocation(
                            LocationManager.NETWORK_PROVIDER
                    );

            if (lastNetwork != null) {

                currentLat =
                        lastNetwork.getLatitude();

                currentLng =
                        lastNetwork.getLongitude();

                updateCurrentMarker();

                mapView.getController()
                        .setCenter(
                                new GeoPoint(
                                        currentLat,
                                        currentLng
                                )
                        );
            }

            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000,
                    2,
                    locationListener,
                    Looper.getMainLooper()
            );

            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    2000,
                    2,
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
                    "📍 Your current location"
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

        updateCurrentMarker();

        mapView.getController()
                .animateTo(point);

        mapView.getController()
                .setZoom(17.0);

        statusText.setText(
                String.format(
                        Locale.US,
                        "📍 Current location: %.6f, %.6f",
                        currentLat,
                        currentLng
                )
        );
    }

    private void searchPlace() {

        final String query =
                searchInput.getText()
                        .toString()
                        .trim();

        if (query.isEmpty()) {

            Toast.makeText(
                    this,
                    "Enter a place first.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        statusText.setText(
                "🔎 Searching for " + query + "..."
        );

        resultsContainer.removeAllViews();

        executor.execute(() -> {

            List<SearchResult> results =
                    new ArrayList<>();

            /*
             * Try Nominatim first for broad place/address results.
             */
            searchNominatim(
                    query,
                    results
            );

            /*
             * If Nominatim returns nothing,
             * try Photon as fallback.
             */
            if (results.isEmpty()) {

                searchPhoton(
                        query,
                        results
                );
            }

            handler.post(() -> {

                if (results.isEmpty()) {

                    statusText.setText(
                            "❌ No results. Try the place name with city or barangay."
                    );

                    Toast.makeText(
                            this,
                            "No search results found.",
                            Toast.LENGTH_LONG
                    ).show();

                    return;
                }

                statusText.setText(
                        "📍 " + results.size()
                                + " result(s) found"
                );

                displayResults(
                        results
                );
            });
        });
    }

    private void searchNominatim(
            String query,
            List<SearchResult> results) {

        HttpURLConnection connection = null;

        try {

            String encoded =
                    URLEncoder.encode(
                            query + ", Philippines",
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
                    "SakayNaAndroid/1.0"
            );

            connection.setConnectTimeout(
                    15000
            );

            connection.setReadTimeout(
                    15000
            );

            int responseCode =
                    connection.getResponseCode();

            if (responseCode != 200) {
                return;
            }

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
                        item.optString(
                                "name",
                                ""
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

            connection.setRequestProperty(
                    "User-Agent",
                    "SakayNaAndroid/1.0"
            );

            connection.setConnectTimeout(
                    15000
            );

            connection.setReadTimeout(
                    15000
            );

            int responseCode =
                    connection.getResponseCode();

            if (responseCode != 200) {
                return;
            }

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
                        properties == null
                                ? ""
                                : properties.optString(
                                        "name",
                                        ""
                                );

                String city =
                        properties == null
                                ? ""
                                : properties.optString(
                                        "city",
                                        ""
                                );

                String street =
                        properties == null
                                ? ""
                                : properties.optString(
                                        "street",
                                        ""
                                );

                String address =
                        joinParts(
                                street,
                                city
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

    private String joinParts(
            String first,
            String second) {

        if (first == null
                || first.trim().isEmpty()) {
            return second == null
                    ? ""
                    : second;
        }

        if (second == null
                || second.trim().isEmpty()) {
            return first;
        }

        return first + ", " + second;
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
                    15,
                    12,
                    15,
                    12
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
                    10
            );

            card.setLayoutParams(
                    params
            );

            TextView name =
                    new TextView(this);

            name.setText(
                    "📍 " + result.name
            );

            name.setTextSize(17);
            name.setTextColor(
                    Color.BLACK
            );

            card.addView(name);

            TextView address =
                    new TextView(this);

            address.setText(
                    result.address
            );

            address.setTextSize(13);
            address.setTextColor(
                    Color.DKGRAY
            );

            card.addView(address);

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

        mapView.getController()
                .animateTo(
                        new GeoPoint(
                                destinationLat,
                                destinationLng
                        )
                );

        mapView.getController()
                .setZoom(17.0);

        mapView.invalidate();

        statusText.setText(
                "✅ Selected: "
                        + destinationName
        );
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
            return;
        }

        rideListener =
                db.collection(
                        "rides"
                )
                        .document(
                                rideId
                        )
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

        Double lat =
                getDouble(
                        ride,
                        "destinationLatitude"
                );

        Double lng =
                getDouble(
                        ride,
                        "destinationLongitude"
                );

        if (lat != null && lng != null) {

            destinationLat = lat;
            destinationLng = lng;

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

            selectDestination(
                    new SearchResult(
                            destinationName,
                            destinationAddress,
                            destinationLat,
                            destinationLng
                    )
            );
        }

        String status =
                ride.getString(
                        "status"
                );

        statusText.setText(
                "🚕 Ride status: "
                        + (
                        status == null
                                ? "UNKNOWN"
                                : status
                )
        );

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

    private void listenToDriverLocation(
            String driverId) {

        if (driverLocationListener != null) {
            driverLocationListener.remove();
        }

        driverLocationListener =
                db.collection(
                        "driverLocations"
                )
                        .document(
                                driverId
                        )
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

                                    if (driverMarker != null) {

                                        mapView.getOverlays()
                                                .remove(
                                                        driverMarker
                                                );
                                    }

                                    driverMarker =
                                            new Marker(
                                                    mapView
                                            );

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
                        );
    }

    private Double getDouble(
            DocumentSnapshot snapshot,
            String field) {

        Object value =
                snapshot.get(
                        field
                );

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

        if (requestCode == LOCATION_PERMISSION
                && grantResults.length > 0
                && grantResults[0]
                == PackageManager.PERMISSION_GRANTED) {

            beginLocationUpdates();
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
