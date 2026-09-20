


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
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
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

private String currentAddress = "";
private String currentPlaceName = "";

private boolean mapReady = false;
private boolean searching = false;

private long lastSearchTime = 0;

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
    root.addView(
            title,
            new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            )
    );

    locationText = new TextView(this);
    locationText.setText("📡 Getting GPS location...");
    locationText.setTextSize(15);
    locationText.setTextColor(Color.DKGRAY);
    locationText.setPadding(15, 5, 15, 5);

    root.addView(
            locationText,
            new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            )
    );

    addressText = new TextView(this);
    addressText.setText("Address: waiting for GPS...");
    addressText.setTextSize(15);
    addressText.setTextColor(Color.DKGRAY);
    addressText.setPadding(15, 5, 15, 10);

    root.addView(
            addressText,
            new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            )
    );

    /*
     * SEARCH AREA
     *
     * This is deliberately a fixed native Android view.
     * The WebView is placed BELOW it and cannot cover it.
     */
    if (!mode.equals("LIVE_RIDE")) {

        LinearLayout searchContainer =
                new LinearLayout(this);

        searchContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        searchContainer.setBackgroundColor(
                Color.rgb(245, 245, 245)
        );

        searchContainer.setPadding(
                12,
                8,
                12,
                8
        );

        TextView searchLabel =
                new TextView(this);

        searchLabel.setText(
                "🔎 SEARCH DESTINATION"
        );

        searchLabel.setTextSize(17);
        searchLabel.setTextColor(Color.BLACK);
        searchLabel.setPadding(
                0,
                0,
                0,
                6
        );

        searchContainer.addView(
                searchLabel
        );

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
                15,
                5,
                15,
                5
        );

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

        searchButton.setFocusable(true);
        searchButton.setClickable(true);
        searchButton.setEnabled(true);

        searchButton.setOnClickListener(
                v -> {

                    /*
                     * Hide keyboard only after the
                     * button itself has received the tap.
                     */
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

        searchContainer.addView(
                searchRow,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        58
                )
        );

        root.addView(
                searchContainer,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );
    }

    /*
     * MAP
     */
    webView = new WebView(this);

    WebSettings settings =
            webView.getSettings();

    settings.setJavaScriptEnabled(true);
    settings.setDomStorageEnabled(true);
    settings.setGeolocationEnabled(true);

    /*
     * IMPORTANT:
     * Do not allow the WebView to steal keyboard focus
     * from the native Search EditText.
     *
     * Map touch still works normally.
     */
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

    currentButton.setFocusable(true);
    currentButton.setClickable(true);

    currentButton.setOnClickListener(
            v -> useCurrentLocation()
    );

    root.addView(
            currentButton
    );

    if (!mode.equals("LIVE_RIDE")) {

        Button confirmButton =
                new Button(this);

        confirmButton.setText(
                "✅ USE SELECTED DESTINATION"
        );

        confirmButton.setFocusable(true);
        confirmButton.setClickable(true);

        confirmButton.setOnClickListener(
                v -> confirmDestination()
        );

        root.addView(
                confirmButton
        );
    }

    Button closeButton =
            new Button(this);

    closeButton.setText(
            "CLOSE MAP"
    );

    closeButton.setFocusable(true);
    closeButton.setClickable(true);

    closeButton.setOnClickListener(
            v -> finish()
    );

    root.addView(
            closeButton
    );

    setContentView(root);

    loadMap();
}

private void loadMap() {

    String html =
            "<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
            "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'>" +
            "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>" +
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
            "{maxZoom:19,attribution:'© OpenStreetMap contributors'}" +
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

private void updateMap(
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

        searchInput.requestFocus();

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

    lastSearchTime = now;
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
                        "Place not found. Try the exact place name or tap the map.",
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
                    10
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

    double resultLat =
            best.getLatitude();

    double resultLng =
            best.getLongitude();

    String name = "";

    if (best.getFeatureName() != null) {
        name =
                best.getFeatureName();
    }

    if (name.isEmpty()
            && best.getLocality() != null) {

        name =
                best.getLocality();
    }

    String address =
            buildAndroidAddress(
                    best
            );

    if (address.isEmpty()) {

        String line =
                best.getAddressLine(0);

        if (line != null) {
            address = line;
        }
    }

    if (name.isEmpty()) {
        name = query;
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
                    Locale.US
            );

    for (Address address : results) {

        if (address == null) {
            continue;
        }

        double score = 0;

        String text =
                address.toString()
                        .toLowerCase(
                                Locale.US
                        );

        if (text.contains(q)) {
            score += 300;
        }

        String feature =
                address.getFeatureName();

        if (feature != null
                && feature.toLowerCase(
                Locale.US
        ).contains(q)) {

            score += 300;
        }

        String locality =
                address.getLocality();

        if (locality != null
                && locality.toLowerCase(
                Locale.US
        ).contains(q)) {

            score += 100;
        }

        String country =
                address.getCountryName();

        if (country != null
                && country.toLowerCase(
                Locale.US
        ).contains("philippines")) {

            score += 100;
        }

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

        if (score > bestScore) {

            bestScore = score;
            best = address;
        }
    }

    return best;
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

    return result.toString();
}

private void applySearchResult(
        JSONObject finalResult) {

    try {

        double resultLat =
                finalResult.getDouble(
                        "lat"
                );

        double resultLng =
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

        if (name.isEmpty()) {

            name =
                    searchInput.getText()
                            .toString()
                            .trim();
        }

        if (address.isEmpty()) {
            address = name;
        }

        currentLat =
                resultLat;

        currentLng =
                resultLng;

        currentPlaceName =
                name;

        currentAddress =
                address;

        updateCoordinates(
                resultLat,
                resultLng
        );

        addressText.setText(
                "📍 "
                        + name
                        + "\n"
                        + address
        );

        final String destinationName =
                name;

        if (webView != null) {

            webView.post(() ->
                    webView.evaluateJavascript(
                            "setDestination("
                                    + resultLat
                                    + ","
                                    + resultLng
                                    + ","
                                    + JSONObject.quote(
                                    destinationName
                            )
                                    + ");",
                            null
                    )
            );
        }

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

private JSONObject searchPhoton(
        String query,
        double lat,
        double lng)
        throws Exception {

    String encoded =
            URLEncoder.encode(
                    query,
                    "UTF-8"
            );

    String urlString =
            "https://photon.komoot.io/api/"
                    + "?q="
                    + encoded
                    + "&limit=8"
                    + "&lat="
                    + lat
                    + "&lon="
                    + lng
                    + "&zoom=14"
                    + "&lang=en";

    JSONObject root =
            getJson(
                    urlString,
                    10000
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
                    query,
                    lat,
                    lng
            );

    if (best == null) {
        return null;
    }

    JSONObject geometry =
            best.getJSONObject(
                    "geometry"
            );

    JSONArray coordinates =
            geometry.getJSONArray(
                    "coordinates"
            );

    double resultLng =
            coordinates.getDouble(0);

    double resultLat =
            coordinates.getDouble(1);

    JSONObject properties =
            best.optJSONObject(
                    "properties"
            );

    String name =
            properties == null
                    ? ""
                    : properties.optString(
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
}

private JSONObject searchNominatim(
        String query,
        double lat,
        double lng)
        throws Exception {

    String encoded =
            URLEncoder.encode(
                    query,
                    "UTF-8"
            );

    String urlString =
            "https://nominatim.openstreetmap.org/search"
                    + "?format=jsonv2"
                    + "&q="
                    + encoded
                    + ", Philippines"
                    + "&limit=8"
                    + "&addressdetails=1"
                    + "&namedetails=1"
                    + "&accept-language=en";

    String response =
            getText(
                    urlString,
                    12000
            );

    JSONArray results =
            new JSONArray(response);

    if (results.length() == 0) {
        return null;
    }

    JSONObject best =
            results.getJSONObject(0);

    double bestDistance =
            Double.MAX_VALUE;

    for (int i = 0;
         i < results.length();
         i++) {

        JSONObject item =
                results.getJSONObject(i);

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
        ).contains(
                query.toLowerCase(
                        Locale.US
                )
        )) {

            score -= 1000;
        }

        if (score < bestDistance) {

            bestDistance = score;
            best = item;
        }
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
}

private JSONObject getJson(
        String urlString,
        int timeout)
        throws Exception {

    String text =
            getText(
                    urlString,
                    timeout
            );

    return new JSONObject(text);
}

private String getText(
        String urlString,
        int timeout)
        throws Exception {

    HttpURLConnection connection =
            null;

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

private JSONObject chooseBestPhoton(
        JSONArray features,
        String query,
        double lat,
        double lng) {

    JSONObject best =
            features.optJSONObject(0);

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

        String lowerName =
                name.toLowerCase(
                        Locale.US
                );

        if (!name.isEmpty()
                && lowerName.equals(q)) {

            score += 300;
        }

        if (!name.isEmpty()
                && lowerName.contains(q)) {

            score += 200;
        }

        if (!name.isEmpty()
                && q.contains(lowerName)) {

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

                double itemLng =
                        c.optDouble(0);

                double itemLat =
                        c.optDouble(1);

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
        }

        if (score > bestScore) {

            bestScore = score;
            best = item;
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

                    if (formatted.isEmpty()) {

                        formatted =
                                address.getAddressLine(0);
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

                        if (!finalName.isEmpty()) {

                            addressText.setText(
                                    "📍 "
                                            + finalName
                                            + "\n"
                                            + finalAddress
                            );

                        } else {

                            addressText.setText(
                                    "📍 "
                                            + finalAddress
                            );
                        }
                    });

                    return;
                }
            }

        } catch (Exception ignored) {
        }

        try {

            String urlString =
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
                            urlString,
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

            final String finalName =
                    name;

            final String finalAddress =
                    formatted;

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

                if (!finalName.isEmpty()) {

                    addressText.setText(
                            "📍 "
                                    + finalName
                                    + "\n"
                                    + finalAddress
                    );

                } else {

                    addressText.setText(
                            "📍 "
                                    + finalAddress
                    );
                }
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

    builder.append(value.trim());
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

private String safe(String value) {

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
