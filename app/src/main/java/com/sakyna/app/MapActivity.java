
package com.sakyna.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
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

        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);

        searchInput = new EditText(this);
        searchInput.setHint(
                "Search Jollibee, SM, street..."
        );
        searchInput.setSingleLine(true);

        Button searchButton = new Button(this);
        searchButton.setText("SEARCH");

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

        searchRow.addView(searchButton);

        root.addView(searchRow);
    }

    webView = new WebView(this);

    WebSettings settings = webView.getSettings();
    settings.setJavaScriptEnabled(true);
    settings.setDomStorageEnabled(true);
    settings.setGeolocationEnabled(true);

    webView.setWebViewClient(new WebViewClient());

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
            "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'>" +
            "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>" +
            "<style>html,body,#map{height:100%;margin:0;padding:0;}</style>" +
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

                    currentLocation = location;

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

/*
 * SEARCH:
 * 1. Try Photon.
 * 2. If Photon is blocked/unavailable,
 *    automatically try Nominatim search.
 * 3. If both fail, GPS/map remain untouched.
 */
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

    new Thread(() -> {

        JSONObject result = null;

        try {

            result =
                    searchPhoton(
                            query,
                            lat,
                            lng
                    );

        } catch (Exception ignored) {
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
                        "Search service unavailable. Try again shortly.",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

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
                    name = address;
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

                if (webView != null) {

                    webView.evaluateJavascript(
                            "setDestination("
                                    + resultLat
                                    + ","
                                    + resultLng
                                    + ","
                                    + JSONObject.quote(
                                    name
                            )
                                    + ");",
                            null
                    );
                }

            } catch (Exception e) {

                Toast.makeText(
                        this,
                        "Invalid search result.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

    }).start();
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

    JSONObject dummy =
            new JSONObject();

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

        connection.setRequestProperty(
                "User-Agent",
                "SakayNa/1.0 Android"
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

        if (grantResults.length > 0
                && grantResults[0]
                == PackageManager.PERMISSION_GRANTED) {

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
