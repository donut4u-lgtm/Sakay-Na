
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class MapActivity extends Activity {

private static final int LOCATION_PERMISSION = 4001;

private WebView webView;
private TextView locationText;
private TextView addressText;
private EditText searchInput;

private LocationManager locationManager;
private LocationListener locationListener;

private FirebaseFirestore db;
private FirebaseAuth auth;

private ListenerRegistration rideListener;
private ListenerRegistration driverLocationListener;

private String mode = "";
private String rideId = "";

private boolean mapReady = false;
private Location currentLocation;

private double currentLat = 0.0;
private double currentLng = 0.0;

private String currentAddress = "";
private String currentPlaceName = "";

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    db = FirebaseFirestore.getInstance();
    auth = FirebaseAuth.getInstance();

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
    title.setPadding(10, 20, 10, 20);

    root.addView(title);

    locationText = new TextView(this);
    locationText.setText(
            "📡 Getting your GPS location..."
    );
    locationText.setTextSize(15);
    locationText.setTextColor(Color.DKGRAY);
    locationText.setPadding(15, 8, 15, 8);

    root.addView(locationText);

    addressText = new TextView(this);
    addressText.setText(
            "Address: waiting for GPS..."
    );
    addressText.setTextSize(15);
    addressText.setTextColor(Color.DKGRAY);
    addressText.setPadding(15, 5, 15, 10);

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
                "Search place, Jollibee, SM..."
        );

        searchInput.setSingleLine(true);

        Button searchButton =
                new Button(this);

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

        searchRow.addView(
                searchButton,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        root.addView(searchRow);
    }

    webView = new WebView(this);

    WebSettings settings =
            webView.getSettings();

    settings.setJavaScriptEnabled(true);
    settings.setDomStorageEnabled(true);
    settings.setDatabaseEnabled(true);
    settings.setBuiltInZoomControls(true);
    settings.setDisplayZoomControls(false);
    settings.setGeolocationEnabled(true);

    webView.setWebViewClient(
            new WebViewClient()
    );

    root.addView(
            webView,
            new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1
            )
    );

    Button myLocationButton =
            new Button(this);

    myLocationButton.setText(
            "📍 USE MY CURRENT LOCATION"
    );

    myLocationButton.setOnClickListener(
            v -> useCurrentLocation()
    );

    root.addView(myLocationButton);

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
            "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "<link rel='stylesheet' " +
            "href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'>" +
            "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>" +
            "<style>" +
            "html,body,#map{height:100%;margin:0;padding:0;}" +
            "</style>" +
            "</head>" +
            "<body>" +
            "<div id='map'></div>" +
            "<script>" +

            "var map = L.map('map').setView([14.5995,120.9842],12);" +

            "L.tileLayer(" +
            "'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'," +
            "{maxZoom:19," +
            "attribution:'© OpenStreetMap contributors'}" +
            ").addTo(map);" +

            "var gpsMarker=null;" +
            "var destinationMarker=null;" +
            "var driverMarker=null;" +

            "function setGPS(lat,lng){" +
            " if(gpsMarker==null){" +
            "   gpsMarker=L.marker([lat,lng]).addTo(map);" +
            " }else{" +
            "   gpsMarker.setLatLng([lat,lng]);" +
            " }" +
            " gpsMarker.bindPopup('📍 Your current GPS location').openPopup();" +
            " map.setView([lat,lng],17);" +
            "}" +

            "function setDestination(lat,lng,name){" +
            " if(destinationMarker==null){" +
            "   destinationMarker=L.marker([lat,lng]).addTo(map);" +
            " }else{" +
            "   destinationMarker.setLatLng([lat,lng]);" +
            " }" +
            " destinationMarker.bindPopup(name || 'Destination').openPopup();" +
            " map.setView([lat,lng],17);" +
            "}" +

            "function setDriver(lat,lng){" +
            " if(driverMarker==null){" +
            "   driverMarker=L.marker([lat,lng]).addTo(map);" +
            " }else{" +
            "   driverMarker.setLatLng([lat,lng]);" +
            " }" +
            " driverMarker.bindPopup('🚕 Driver location');" +
            "}" +

            "map.on('click',function(e){" +
            " AndroidMapTap(e.latlng.lat,e.latlng.lng);" +
            "});" +

            "</script>" +
            "</body>" +
            "</html>";

    webView.getSettings()
            .setJavaScriptEnabled(true);

    webView.addJavascriptInterface(
            new MapBridge(),
            "AndroidMap"
    );

    webView.loadDataWithBaseURL(
            "https://www.openstreetmap.org/",
            html,
            "text/html",
            "UTF-8",
            null
    );

    mapReady = true;

    if (currentLocation != null) {
        updateMapLocation(
                currentLocation
        );
    }
}

private class MapBridge {

    @android.webkit.JavascriptInterface
    public void AndroidMapTap(
            double lat,
            double lng) {

        runOnUiThread(
                () -> {

                    currentLat = lat;
                    currentLng = lng;

                    updateCoordinatesText(
                            lat,
                            lng
                    );

                    reverseGeocode(
                            lat,
                            lng,
                            true
                    );
                }
        );
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

    if (locationManager == null) {
        return;
    }

    locationListener =
            new LocationListener() {

                @Override
                public void onLocationChanged(
                        @NonNull Location location) {

                    /*
                     * GPS is authoritative.
                     * Never replace the coordinates with
                     * a geocoder result.
                     */
                    currentLocation = location;

                    currentLat =
                            location.getLatitude();

                    currentLng =
                            location.getLongitude();

                    updateCoordinatesText(
                            currentLat,
                            currentLng
                    );

                    updateMapLocation(
                            location
                    );

                    /*
                     * Address lookup is separate from
                     * physical GPS position.
                     */
                    reverseGeocode(
                            currentLat,
                            currentLng,
                            false
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
                chooseBestLocation(
                        gps,
                        network
                );

        if (best != null) {

            currentLocation = best;

            currentLat =
                    best.getLatitude();

            currentLng =
                    best.getLongitude();

            updateCoordinatesText(
                    currentLat,
                    currentLng
            );

            updateMapLocation(best);

            reverseGeocode(
                    currentLat,
                    currentLng,
                    false
            );
        } else {

            locationText.setText(
                    "📡 Waiting for a new GPS fix..."
            );
        }

    } catch (SecurityException e) {

        locationText.setText(
                "📍 Location permission is required."
        );
    }
}

private Location chooseBestLocation(
        Location first,
        Location second) {

    if (first == null) {
        return second;
    }

    if (second == null) {
        return first;
    }

    if (first.hasAccuracy()
            && second.hasAccuracy()) {

        return first.getAccuracy()
                <= second.getAccuracy()
                ? first
                : second;
    }

    return first;
}

private void updateCoordinatesText(
        double lat,
        double lng) {

    locationText.setText(
            "📍 GPS LOCATION\n"
                    + "Latitude: "
                    + String.format(
                    java.util.Locale.US,
                    "%.6f",
                    lat
            )
                    + "\nLongitude: "
                    + String.format(
                    java.util.Locale.US,
                    "%.6f",
                    lng
            )
    );
}

private void updateMapLocation(
        Location location) {

    if (location == null
            || webView == null
            || !mapReady) {
        return;
    }

    double lat =
            location.getLatitude();

    double lng =
            location.getLongitude();

    String javascript =
            "setGPS("
                    + lat
                    + ","
                    + lng
                    + ");";

    webView.post(
            () -> webView.evaluateJavascript(
                    javascript,
                    null
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

    updateCoordinatesText(
            currentLat,
            currentLng
    );

    updateMapLocation(
            currentLocation
    );

    reverseGeocode(
            currentLat,
            currentLng,
            true
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

    final double biasLat =
            currentLocation == null
                    ? 14.5995
                    : currentLocation.getLatitude();

    final double biasLng =
            currentLocation == null
                    ? 120.9842
                    : currentLocation.getLongitude();

    new Thread(() -> {

        HttpURLConnection connection =
                null;

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
                            + "&limit=8"
                            + "&lat="
                            + biasLat
                            + "&lon="
                            + biasLng
                            + "&zoom=18";

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
                    "SakayNa/1.0"
            );

            InputStream input =
                    connection.getInputStream();

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    input
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

            JSONObject root =
                    new JSONObject(
                            result.toString()
                    );

            JSONArray features =
                    root.getJSONArray(
                            "features"
                    );

            if (features.length() == 0) {

                runOnUiThread(
                        () -> Toast.makeText(
                                this,
                                "Place not found.",
                                Toast.LENGTH_SHORT
                        ).show()
                );

                return;
            }

            JSONObject best =
                    features.getJSONObject(0);

            JSONObject geometry =
                    best.getJSONObject(
                            "geometry"
                    );

            JSONArray coordinates =
                    geometry.getJSONArray(
                            "coordinates"
                    );

            double lng =
                    coordinates.getDouble(0);

            double lat =
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

            if (name.isEmpty()) {
                name = address;
            }

            final String finalName =
                    name;

            final String finalAddress =
                    address.isEmpty()
                            ? name
                            : address;

            final double finalLat =
                    lat;

            final double finalLng =
                    lng;

            runOnUiThread(
                    () -> {

                        currentLat =
                                finalLat;

                        currentLng =
                                finalLng;

                        currentPlaceName =
                                finalName;

                        currentAddress =
                                finalAddress;

                        updateCoordinatesText(
                                finalLat,
                                finalLng
                        );

                        addressText.setText(
                                "📍 "
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
                    }
            );

        } catch (Exception e) {

            runOnUiThread(
                    () -> Toast.makeText(
                            this,
                            "Search failed.",
                            Toast.LENGTH_SHORT
                    ).show()
            );

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }

    }).start();
}

private String buildPhotonAddress(
        JSONObject properties) {

    if (properties == null) {
        return "";
    }

    StringBuilder address =
            new StringBuilder();

    addPart(
            address,
            properties.optString(
                    "housenumber",
                    ""
            )
    );

    addPart(
            address,
            properties.optString(
                    "street",
                    ""
            )
    );

    addPart(
            address,
            properties.optString(
                    "district",
                    ""
            )
    );

    addPart(
            address,
            properties.optString(
                    "city",
                    ""
            )
    );

    addPart(
            address,
            properties.optString(
                    "state",
                    ""
            )
    );

    return address.toString();
}

private void reverseGeocode(
        double lat,
        double lng,
        boolean moveMap) {

    new Thread(() -> {

        HttpURLConnection connection =
                null;

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
                    "SakayNa/1.0"
            );

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

            JSONObject object =
                    new JSONObject(
                            result.toString()
                    );

            JSONObject address =
                    object.optJSONObject(
                            "address"
                    );

            String named =
                    extractBestName(
                            object
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

            final String finalName =
                    named;

            final String finalAddress =
                    formatted;

            runOnUiThread(
                    () -> {

                        /*
                         * IMPORTANT:
                         * lat/lng are NEVER replaced
                         * by the address lookup.
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

                        String display =
                                finalName.isEmpty()
                                        ? finalAddress
                                        : finalName
                                        + "\n"
                                        + finalAddress;

                        addressText.setText(
                                display.isEmpty()
                                        ? "Address unavailable"
                                        : "📍 "
                                        + display
                        );

                        if (moveMap
                                && webView != null) {

                            webView.evaluateJavascript(
                                    "setDestination("
                                            + lat
                                            + ","
                                            + lng
                                            + ","
                                            + JSONObject.quote(
                                            finalName.isEmpty()
                                                    ? finalAddress
                                                    : finalName
                                    )
                                            + ");",
                                    null
                            );
                        }
                    }
            );

        } catch (Exception ignored) {

            runOnUiThread(
                    () -> addressText.setText(
                            "📍 GPS position acquired\n"
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

private String extractBestName(
        JSONObject object) {

    String name =
            object.optString(
                    "name",
                    ""
            );

    if (!name.isEmpty()) {
        return name;
    }

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

        if (!name.isEmpty()) {
            return name;
        }

        name =
                namedetails.optString(
                        "brand",
                        ""
                );
    }

    return name;
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

    String name =
            currentPlaceName;

    String address =
            currentAddress;

    if (name == null) {
        name = "";
    }

    if (address == null) {
        address = "";
    }

    result.putExtra(
            "destinationName",
            name
    );

    result.putExtra(
            "destination_address",
            address
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

private void startLiveRideListener() {

    if (rideId.isEmpty()) {
        return;
    }

    if (rideListener != null) {
        rideListener.remove();
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

                                Object latObject =
                                        snapshot.get(
                                                "driverLatitude"
                                        );

                                Object lngObject =
                                        snapshot.get(
                                                "driverLongitude"
                                        );

                                if (latObject != null
                                        && lngObject != null) {

                                    double lat =
                                            Double.parseDouble(
                                                    String.valueOf(
                                                            latObject
                                                    )
                                            );

                                    double lng =
                                            Double.parseDouble(
                                                    String.valueOf(
                                                            lngObject
                                                    )
                                            );

                                    showDriverMarker(
                                            lat,
                                            lng
                                    );
                                }

                                listenForDriverLocation(
                                        snapshot
                                );
                            }
                    );
}

private void listenForDriverLocation(
        DocumentSnapshot ride) {

    String driverId =
            ride.getString(
                    "driverId"
            );

    if (driverId == null
            || driverId.isEmpty()) {
        return;
    }

    if (driverLocationListener != null) {
        driverLocationListener.remove();
    }

    driverLocationListener =
            db.collection("driverLocations")
                    .document(driverId)
                    .addSnapshotListener(
                            (snapshot, error) -> {

                                if (error != null
                                        || snapshot == null
                                        || !snapshot.exists()) {
                                    return;
                                }

                                Object latObject =
                                        snapshot.get(
                                                "latitude"
                                        );

                                Object lngObject =
                                        snapshot.get(
                                                "longitude"
                                        );

                                if (latObject == null
                                        || lngObject == null) {
                                    return;
                                }

                                double lat =
                                        Double.parseDouble(
                                                String.valueOf(
                                                        latObject
                                                )
                                        );

                                double lng =
                                        Double.parseDouble(
                                                String.valueOf(
                                                        lngObject
                                                )
                                        );

                                showDriverMarker(
                                        lat,
                                        lng
                                );
                            }
                    );
}

private void showDriverMarker(
        double lat,
        double lng) {

    if (webView == null) {
        return;
    }

    webView.post(
            () -> webView.evaluateJavascript(
                    "setDriver("
                            + lat
                            + ","
                            + lng
                            + ");",
                    null
            )
    );
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

    if (requestCode == LOCATION_PERMISSION) {

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

    if (rideListener != null) {
        rideListener.remove();
        rideListener = null;
    }

    if (driverLocationListener != null) {
        driverLocationListener.remove();
        driverLocationListener = null;
    }

    if (webView != null) {
        webView.stopLoading();
        webView.destroy();
        webView = null;
    }

    super.onDestroy();
}

}
