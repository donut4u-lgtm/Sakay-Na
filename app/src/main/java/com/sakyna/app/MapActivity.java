
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
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
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

private boolean searching = false;
private long lastSearchTime = 0;

private String mode = "";
private String rideId = "";

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Configuration.getInstance()
            .setUserAgentValue(
                    "SakayNa/1.0"
            );

    auth = FirebaseAuth.getInstance();
    db = FirebaseFirestore.getInstance();

    Intent intent = getIntent();

    if (intent != null) {
        mode = safe(
                intent.getStringExtra("mode")
        );

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

    root.setBackgroundColor(
            Color.WHITE
    );

    TextView title =
            new TextView(this);

    title.setText(
            mode.equals("LIVE_RIDE")
                    ? "🚕 LIVE RIDE MAP"
                    : "📍 SELECT DESTINATION"
    );

    title.setTextSize(22);
    title.setTextColor(Color.BLACK);
    title.setGravity(Gravity.CENTER);
    title.setPadding(
            10,
            18,
            10,
            18
    );

    root.addView(title);

    locationText =
            new TextView(this);

    locationText.setText(
            "📡 Getting GPS location..."
    );

    locationText.setTextSize(15);
    locationText.setTextColor(
            Color.DKGRAY
    );

    locationText.setPadding(
            15,
            5,
            15,
            5
    );

    root.addView(locationText);

    addressText =
            new TextView(this);

    addressText.setText(
            "Address: waiting for GPS..."
    );

    addressText.setTextSize(15);
    addressText.setTextColor(
            Color.DKGRAY
    );

    addressText.setPadding(
            15,
            5,
            15,
            10
    );

    root.addView(addressText);

    if (!mode.equals("LIVE_RIDE")) {

        LinearLayout searchRow =
                new LinearLayout(this);

        searchRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        searchRow.setPadding(
                10,
                5,
                10,
                8
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

        searchButton.setText(
                "SEARCH"
        );

        searchButton.setTextColor(
                Color.WHITE
        );

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

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        125,
                        58
                );

        buttonParams.setMargins(
                8,
                0,
                0,
                0
        );

        searchRow.addView(
                searchButton,
                buttonParams
        );

        root.addView(searchRow);
    }

    mapView =
            new MapView(this);

    mapView.setTileSource(
            TileSourceFactory.MAPNIK
    );

    mapView.setMultiTouchControls(
            true
    );

    mapView.setBuiltInZoomControls(
            true
    );

    mapView.setMinZoomLevel(3.0);

    mapView.setMaxZoomLevel(19.0);

    mapView.setBackgroundColor(
            Color.rgb(235, 235, 235)
    );

    GeoPoint defaultPoint =
            new GeoPoint(
                    14.5995,
                    120.9842
            );

    mapView.getController()
            .setZoom(12.0);

    mapView.getController()
            .setCenter(defaultPoint);

    MapEventsReceiver receiver =
            new MapEventsReceiver() {

                @Override
                public boolean singleTapConfirmedHelper(
                        GeoPoint point) {

                    currentLat =
                            point.getLatitude();

                    currentLng =
                            point.getLongitude();

                    setDestinationMarker(
                            currentLat,
                            currentLng,
                            "Selected destination"
                    );

                    reverseGeocode(
                            currentLat,
                            currentLng
                    );

                    return true;
                }

                @Override
                public boolean longPressHelper(
                        GeoPoint point) {

                    return false;
                }
            };

    mapView.getOverlays().add(
            new MapEventsOverlay(
                    receiver
            )
    );

    LinearLayout.LayoutParams mapParams =
            new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1
            );

    root.addView(
            mapView,
            mapParams
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

    closeButton.setText(
            "CLOSE MAP"
    );

    closeButton.setOnClickListener(
            v -> finish()
    );

    root.addView(closeButton);

    setContentView(root);
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

                    setGpsMarker(
                            currentLat,
                            currentLng
                    );

                    if (currentPlaceName.isEmpty()
                            && currentAddress.isEmpty()) {

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

            currentLocation =
                    best;

            currentLat =
                    best.getLatitude();

            currentLng =
                    best.getLongitude();

            updateCoordinates(
                    currentLat,
                    currentLng
            );

            setGpsMarker(
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

    if (a == null) {
        return b;
    }

    if (b == null) {
        return a;
    }

    if (a.hasAccuracy()
            && b.hasAccuracy()) {

        return a.getAccuracy()
                <= b.getAccuracy()
                ? a
                : b;
    }

    return a;
}

private void setGpsMarker(
        double lat,
        double lng) {

    if (mapView == null) {
        return;
    }

    GeoPoint point =
            new GeoPoint(
                    lat,
                    lng
            );

    if (gpsMarker == null) {

        gpsMarker =
                new Marker(
                        mapView
                );

        gpsMarker.setTitle(
                "📍 Your location"
        );

        mapView.getOverlays()
                .add(gpsMarker);
    }

    gpsMarker.setPosition(
            point
    );

    gpsMarker.setAnchor(
            Marker.ANCHOR_CENTER,
            Marker.ANCHOR_BOTTOM
    );

    mapView.getController()
            .setCenter(point);

    mapView.getController()
            .setZoom(17.0);

    mapView.invalidate();
}

private void setDestinationMarker(
        double lat,
        double lng,
        String name) {

    if (mapView == null) {
        return;
    }

    GeoPoint point =
            new GeoPoint(
                    lat,
                    lng
            );

    if (destinationMarker == null) {

        destinationMarker =
                new Marker(
                        mapView
                );

        mapView.getOverlays()
                .add(destinationMarker);
    }

    destinationMarker.setPosition(
            point
    );

    destinationMarker.setTitle(
            name == null
                    ? "Destination"
                    : name
    );

    destinationMarker.setAnchor(
            Marker.ANCHOR_CENTER,
            Marker.ANCHOR_BOTTOM
    );

    mapView.getController()
            .animateTo(point);

    mapView.getController()
            .setZoom(17.0);

    mapView.invalidate();
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

private void useCurrentLocation() {

    if (currentLocation == null) {

        Toast.makeText(
                this,
                "Waiting for GPS location...",
                Toast.LENGTH_SHORT
        ).show();

        return;
    }

    currentLat =
            currentLocation.getLatitude();

    currentLng =
            currentLocation.getLongitude();

    currentPlaceName =
            "Current location";

    setDestinationMarker(
            currentLat,
            currentLng,
            currentPlaceName
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

    if (searching) {

        Toast.makeText(
                this,
                "Search is loading...",
                Toast.LENGTH_SHORT
        ).show();

        return;
    }

    long now =
            System.currentTimeMillis();

    if (now - lastSearchTime < 1000) {

        Toast.makeText(
                this,
                "Please wait a moment.",
                Toast.LENGTH_SHORT
        ).show();

        return;
    }

    lastSearchTime =
            now;

    searching = true;

    final double lat =
            currentLocation == null
                    ? 14.5995
                    : currentLocation.getLatitude();

    final double lng =
            currentLocation == null
                    ? 120.9842
                    : currentLocation.getLongitude();

    Toast.makeText(
            this,
            "🔎 Searching...",
            Toast.LENGTH_SHORT
    ).show();

    new Thread(() -> {

        JSONObject result = null;

        try {
            result =
                    searchAndroidGeocoder(
                            query,
                            lat,
                            lng
                    );
        } catch (Exception ignored) {
        }

        if (result == null) {

            try {
                result =
                        searchPhoton(
                                query,
                                lat,
                                lng
                        );
            } catch (Exception ignored) {
            }
        }

        if (result == null) {

            try {
                result =
                        searchNominatim(
                                query,
                                lat,
                                lng
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
        double lng) throws Exception {

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
                    8,
                    13.0,
                    120.0,
                    15.0,
                    122.5
            );

    if (results == null
            || results.isEmpty()) {
        return null;
    }

    Address best =
            chooseBestAndroidAddress(
                    results,
                    query,
                    lat,
                    lng
            );

    if (best == null) {
        return null;
    }

    JSONObject object =
            new JSONObject();

    object.put(
            "lat",
            best.getLatitude()
    );

    object.put(
            "lon",
            best.getLongitude()
    );

    String name =
            best.getFeatureName();

    String address =
            best.getAddressLine(0);

    object.put(
            "name",
            name == null
                    ? query
                    : name
    );

    object.put(
            "address",
            address == null
                    ? query
                    : address
    );

    return object;
}

private Address chooseBestAndroidAddress(
        List<Address> results,
        String query,
        double lat,
        double lng) {

    Address best = null;
    double bestScore =
            -Double.MAX_VALUE;

    String q =
            query.toLowerCase(
                    Locale.ENGLISH
            );

    for (Address address : results) {

        if (address == null) {
            continue;
        }

        String text =
                address.toString()
                        .toLowerCase(
                                Locale.ENGLISH
                        );

        double distance =
                distanceKm(
                        lat,
                        lng,
                        address.getLatitude(),
                        address.getLongitude()
                );

        double score =
                -distance * 10.0;

        if (text.contains(q)) {
            score += 1000.0;
        }

        if (best == null
                || score > bestScore) {

            best = address;
            bestScore = score;
        }
    }

    return best;
}

private JSONObject searchPhoton(
        String query,
        double lat,
        double lng) throws Exception {

    String urlText =
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

    JSONObject response =
            getJson(
                    urlText,
                    10000
            );

    JSONArray features =
            response.optJSONArray(
                    "features"
            );

    if (features == null
            || features.length() == 0) {
        return null;
    }

    JSONObject best =
            null;

    double bestScore =
            -Double.MAX_VALUE;

    String q =
            query.toLowerCase(
                    Locale.ENGLISH
            );

    for (int i = 0;
            i < features.length();
            i++) {

        JSONObject feature =
                features.optJSONObject(i);

        if (feature == null) {
            continue;
        }

        JSONObject geometry =
                feature.optJSONObject(
                        "geometry"
                );

        JSONObject properties =
                feature.optJSONObject(
                        "properties"
                );

        if (geometry == null
                || properties == null) {
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

        double resultLng =
                coordinates.optDouble(0);

        double resultLat =
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

        String street =
                properties.optString(
                        "street",
                        ""
                );

        String country =
                properties.optString(
                        "country",
                        ""
                );

        String text =
                (
                        name
                                + " "
                                + street
                                + " "
                                + city
                                + " "
                                + country
                ).toLowerCase(
                        Locale.ENGLISH
                );

        double distance =
                distanceKm(
                        lat,
                        lng,
                        resultLat,
                        resultLng
                );

        double score =
                -distance * 10.0;

        if (text.contains(q)) {
            score += 1000.0;
        }

        if (name
                .toLowerCase(
                        Locale.ENGLISH
                )
                .equals(q)) {

            score += 500.0;
        }

        if (best == null
                || score > bestScore) {

            JSONObject selected =
                    new JSONObject();

            selected.put(
                    "lat",
                    resultLat
            );

            selected.put(
                    "lon",
                    resultLng
            );

            selected.put(
                    "name",
                    name
            );

            String address =
                    buildPhotonAddress(
                            properties
                    );

            selected.put(
                    "address",
                    address
            );

            best = selected;
            bestScore = score;
        }
    }

    return best;
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

private JSONObject searchNominatim(
        String query,
        double lat,
        double lng) throws Exception {

    String urlText =
            "https://nominatim.openstreetmap.org/search"
                    + "?format=jsonv2"
                    + "&q="
                    + URLEncoder.encode(
                            query
                                    + ", Philippines",
                            "UTF-8"
                    )
                    + "&limit=8"
                    + "&addressdetails=1"
                    + "&namedetails=1"
                    + "&accept-language=en";

    JSONArray results =
            getJsonArray(
                    urlText,
                    10000
            );

    if (results == null
            || results.length() == 0) {
        return null;
    }

    JSONObject best =
            null;

    double bestScore =
            -Double.MAX_VALUE;

    String q =
            query.toLowerCase(
                    Locale.ENGLISH
            );

    for (int i = 0;
            i < results.length();
            i++) {

        JSONObject item =
                results.optJSONObject(i);

        if (item == null) {
            continue;
        }

        double resultLat =
                item.optDouble(
                        "lat",
                        0
                );

        double resultLng =
                item.optDouble(
                        "lon",
                        0
                );

        String display =
                item.optString(
                        "display_name",
                        ""
                );

        double distance =
                distanceKm(
                        lat,
                        lng,
                        resultLat,
                        resultLng
                );

        double score =
                -distance * 10.0;

        if (display
                .toLowerCase(
                        Locale.ENGLISH
                )
                .contains(q)) {

            score += 1000.0;
        }

        if (best == null
                || score > bestScore) {

            String name =
                    item.optString(
                            "name",
                            ""
                    );

            if (name.isEmpty()) {

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
                }
            }

            JSONObject selected =
                    new JSONObject();

            selected.put(
                    "lat",
                    resultLat
            );

            selected.put(
                    "lon",
                    resultLng
            );

            selected.put(
                    "name",
                    name
            );

            selected.put(
                    "address",
                    buildNominatimAddress(
                            item.optJSONObject(
                                    "address"
                            )
                    )
            );

            best = selected;
            bestScore = score;
        }
    }

    return best;
}

private void applySearchResult(
        JSONObject result) {

    try {

        double lat =
                result.getDouble(
                        "lat"
                );

        double lng =
                result.getDouble(
                        "lon"
                );

        String name =
                result.optString(
                        "name",
                        ""
                );

        String address =
                result.optString(
                        "address",
                        ""
                );

        if (name.isEmpty()) {

            name =
                    searchInput
                            .getText()
                            .toString()
                            .trim();
        }

        if (address.isEmpty()) {
            address = name;
        }

        currentLat = lat;
        currentLng = lng;

        currentPlaceName =
                name;

        currentAddress =
                address;

        setDestinationMarker(
                lat,
                lng,
                name
        );

        addressText.setText(
                "📍 "
                        + name
                        + "\n"
                        + address
        );

        refreshPlaceLabel(
                lat,
                lng,
                name,
                address
        );

        Toast.makeText(
                this,
                "✅ Place found.",
                Toast.LENGTH_SHORT
        ).show();

    } catch (Exception e) {

        Toast.makeText(
                this,
                "Invalid search result.",
                Toast.LENGTH_SHORT
        ).show();
    }
}

private void refreshPlaceLabel(
        double lat,
        double lng,
        String fallbackName,
        String fallbackAddress) {

    new Thread(() -> {

        try {

            String urlText =
                    "https://nominatim.openstreetmap.org/reverse"
                            + "?lat="
                            + lat
                            + "&lon="
                            + lng
                            + "&format=jsonv2"
                            + "&zoom=18"
                            + "&addressdetails=1"
                            + "&namedetails=1"
                            + "&accept-language=en";

            JSONObject object =
                    getJson(
                            urlText,
                            10000
                    );

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

            if (name.isEmpty()) {
                name = fallbackName;
            }

            if (address.isEmpty()) {
                address = fallbackAddress;
            }

            final String finalName =
                    name;

            final String finalAddress =
                    address;

            runOnUiThread(() -> {

                currentPlaceName =
                        finalName;

                currentAddress =
                        finalAddress;

                addressText.setText(
                        "📍 "
                                + finalName
                                + "\n"
                                + finalAddress
                );
            });

        } catch (Exception ignored) {
        }

    }).start();
}

private void reverseGeocode(
        double lat,
        double lng) {

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

                    String name =
                            address.getFeatureName();

                    if (name == null) {
                        name = "";
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

                        showAddress();
                    });

                    return;
                }
            }

        } catch (Exception ignored) {
        }

        try {

            String urlText =
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
                            urlText,
                            10000
                    );

            String name =
                    object.optString(
                            "name",
                            ""
                    );

            JSONObject namedetails =
                    object.optJSONObject(
                            "namedetails"
                    );

            if (name.isEmpty()
                    && namedetails != null) {

                name =
                        namedetails.optString(
                                "name",
                                ""
                        );
            }

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

                if (!finalName.isEmpty()) {
                    currentPlaceName =
                            finalName;
                }

                if (!finalAddress.isEmpty()) {
                    currentAddress =
                            finalAddress;
                }

                showAddress();
            });

        } catch (Exception ignored) {
        }

    }).start();
}

private void showAddress() {

    if (currentPlaceName.isEmpty()) {

        addressText.setText(
                "📍 "
                        + currentAddress
        );

    } else {

        addressText.setText(
                "📍 "
                        + currentPlaceName
                        + "\n"
                        + currentAddress
        );
    }
}

private String buildAndroidAddress(
        Address address) {

    if (address == null) {
        return "";
    }

    String line =
            address.getAddressLine(0);

    if (line != null
            && !line.trim().isEmpty()) {

        return line.trim();
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
        String urlText,
        int timeout) throws Exception {

    HttpURLConnection connection =
            null;

    try {

        URL url =
                new URL(urlText);

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
                    "HTTP "
                            + code
            );
        }

        BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                connection
                                        .getInputStream()
                        )
                );

        StringBuilder response =
                new StringBuilder();

        String line;

        while ((line =
                reader.readLine()) != null) {

            response.append(line);
        }

        reader.close();

        return new JSONObject(
                response.toString()
        );

    } finally {

        if (connection != null) {
            connection.disconnect();
        }
    }
}

private JSONArray getJsonArray(
        String urlText,
        int timeout) throws Exception {

    HttpURLConnection connection =
            null;

    try {

        URL url =
                new URL(urlText);

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
                    "HTTP "
                            + code
            );
        }

        BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                connection
                                        .getInputStream()
                        )
                );

        StringBuilder response =
                new StringBuilder();

        String line;

        while ((line =
                reader.readLine()) != null) {

            response.append(line);
        }

        reader.close();

        return new JSONArray(
                response.toString()
        );

    } finally {

        if (connection != null) {
            connection.disconnect();
        }
    }
}

private double distanceKm(
        double lat1,
        double lon1,
        double lat2,
        double lon2) {

    double earthRadius =
            6371.0;

    double dLat =
            Math.toRadians(
                    lat2 - lat1
            );

    double dLon =
            Math.toRadians(
                    lon2 - lon1
            );

    double a =
            Math.sin(dLat / 2)
                    * Math.sin(dLat / 2)
                    + Math.cos(
                    Math.toRadians(lat1)
            )
                    * Math.cos(
                    Math.toRadians(lat2)
            )
                    * Math.sin(dLon / 2)
                    * Math.sin(dLon / 2);

    double c =
            2.0
                    * Math.atan2(
                    Math.sqrt(a),
                    Math.sqrt(
                            1.0 - a
                    )
            );

    return earthRadius * c;
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

        boolean granted =
                false;

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
        mapView = null;
    }

    super.onDestroy();
}

}
