
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
import java.net.URLEncoder;
import java.net.URL;
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
    title.setPadding(10, 18, 10, 18);

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
            15, 5, 15, 5
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
            15, 5, 15, 10
    );

    root.addView(addressText);

    if (!mode.equals("LIVE_RIDE")) {

        LinearLayout searchRow =
                new LinearLayout(this);

        searchRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        searchInput =
                new EditText(this);

        searchInput.setHint(
                "Search Jollibee, SM, street..."
        );

        searchInput.setSingleLine(true);

        Button searchButton =
                new Button(this);

        searchButton.setText(
                "SEARCH"
        );

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
                searchButton
        );

        root.addView(searchRow);
    }

    webView =
            new WebView(this);

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
            "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'>" +
            "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>" +
            "<style>" +
            "html,body,#map{height:100%;margin:0;padding:0;}" +
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
            "destinationMarker.bindPopup(name || 'Destination').openPopup();" +
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

        return;
    }

    if (searching) {

        Toast.makeText(
                this,
                "Search is still loading...",
                Toast.LENGTH_SHORT
        ).show();

        return;
    }

    long now =
            System.currentTimeMillis();

    if (now - lastSearchTime < 1200) {

        Toast.makeText(
                this,
                "Please wait a moment before searching again.",
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
                            + "&limit=5"
                            + "&lat="
                            + lat
                            + "&lon="
                            + lng
                            + "&zoom=14"
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
                    12000
            );

            connection.setReadTimeout(
                    12000
            );

            connection.setRequestProperty(
                    "User-Agent",
                    "SakayNa/1.0 Android"
            );

            int responseCode =
                    connection.getResponseCode();

            if (responseCode != 200) {

                throw new Exception(
                        "HTTP "
                                + responseCode
                );
            }

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    connection.getInputStream()
                            )
                    );

            StringBuilder json =
                    new StringBuilder();

            String line;

            while ((line =
                    reader.readLine()) != null) {

                json.append(line);
            }

            reader.close();

            JSONObject root =
                    new JSONObject(
                            json.toString()
                    );

            JSONArray features =
                    root.optJSONArray(
                            "features"
                    );

            if (features == null
                    || features.length() == 0) {

                throw new Exception(
                        "No results"
                );
            }

            JSONObject selected =
                    chooseBestResult(
                            features,
                            query,
                            lat,
                            lng
                    );

            JSONObject geometry =
                    selected.getJSONObject(
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
                    selected.optJSONObject(
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
                    buildAddress(
                            properties
                    );

            if (name.isEmpty()) {
                name = address;
            }

            if (address.isEmpty()) {
                address = name;
            }

            final double finalLat =
                    resultLat;

            final double finalLng =
                    resultLng;

            final String finalName =
                    name;

            final String finalAddress =
                    address;

            runOnUiThread(() -> {

                searching = false;

                currentLat =
                        finalLat;

                currentLng =
                        finalLng;

                currentPlaceName =
                        finalName;

                currentAddress =
                        finalAddress;

                updateCoordinates(
                        finalLat,
                        finalLng
                );

                addressText.setText(
                        "📍 "
                                + finalName
                                + "\n"
                                + finalAddress
                );

                if (webView != null) {

                    webView.evaluateJavascript(
                            "setDestination("
                                    + finalLat
                                    + ","
                                    + finalLng
                                    + ","
                                    + JSONObject.quote(
                                    finalName
                            )
                                    + ");",
                            null
                    );
                }
            });

        } catch (Exception e) {

            runOnUiThread(() -> {

                searching = false;

                Toast.makeText(
                        this,
                        "Search temporarily unavailable. Try again in a few seconds.",
                        Toast.LENGTH_LONG
                ).show();
            });

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }

    }).start();
}

private JSONObject chooseBestResult(
        JSONArray features,
        String query,
        double lat,
        double lng) {

    JSONObject best =
            features.optJSONObject(0);

    double bestScore =
            -Double.MAX_VALUE;

    String queryLower =
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

        JSONObject properties =
                item.optJSONObject(
                        "properties"
                );

        if (properties == null) {
            continue;
        }

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

        if (!name.isEmpty()
                && queryLower.contains(
                name.toLowerCase(
                        Locale.US
                ))) {

            score += 100;
        }

        if (!name.isEmpty()
                && name.toLowerCase(
                Locale.US
        ).contains(queryLower)) {

            score += 100;
        }

        if (country.equalsIgnoreCase(
                "Philippines"
        )) {

            score += 40;
        }

        if (city.equalsIgnoreCase(
                "San Pedro"
        )) {

            score += 15;
        }

        String osmValue =
                properties.optString(
                        "osm_value",
                        ""
                );

        if (osmValue.equals(
                "restaurant"
        )
                || osmValue.equals(
                "fast_food"
        )
                || osmValue.equals(
                "shop"
        )
                || osmValue.equals(
                "supermarket"
        )
                || osmValue.equals(
                "mall"
        )
                || osmValue.equals(
                "school"
        )
                || osmValue.equals(
                "hospital"
        )) {

            score += 25;
        }

        JSONObject geometry =
                item.optJSONObject(
                        "geometry"
                );

        if (geometry != null) {

            JSONArray coordinates =
                    geometry.optJSONArray(
                            "coordinates"
                    );

            if (coordinates != null
                    && coordinates.length() >= 2) {

                double itemLng =
                        coordinates.optDouble(
                                0
                        );

                double itemLat =
                        coordinates.optDouble(
                                1
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

                score -=
                        Math.min(
                                distance[0] / 1000.0,
                                20
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

private String buildAddress(
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

        HttpURLConnection connection = null;

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

            connection.setRequestProperty(
                    "User-Agent",
                    "SakayNa/1.0 Android"
            );

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    connection.getInputStream()
                            )
                    );

            StringBuilder json =
                    new StringBuilder();

            String line;

            while ((line =
                    reader.readLine()) != null) {

                json.append(line);
            }

            reader.close();

            JSONObject object =
                    new JSONObject(
                            json.toString()
                    );

            JSONObject address =
                    object.optJSONObject(
                            "address"
                    );

            String formatted =
                    buildReverseAddress(
                            address
                    );

            if (formatted.isEmpty()) {

                formatted =
                        object.optString(
                                "display_name",
                                ""
                        );
            }

            JSONObject namedetails =
                    object.optJSONObject(
                            "namedetails"
                    );

            String name =
                    object.optString(
                            "name",
                            ""
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
                            "📍 GPS location active\n"
                                    + "Address lookup unavailable"
                    )
            );

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }

    }).start();
}

private String buildReverseAddress(
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

    String barangay =
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
            );

    addPart(
            result,
            barangay
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
