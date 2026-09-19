
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
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
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
private Button searchButton;

private LocationManager locationManager;
private LocationListener locationListener;

private FirebaseAuth auth;
private FirebaseFirestore db;

private Location currentLocation;

private double currentLat = 0.0;
private double currentLng = 0.0;

private double selectedLat = 0.0;
private double selectedLng = 0.0;

private String currentAddress = "";
private String currentPlaceName = "";

private String selectedAddress = "";
private String selectedPlaceName = "";

private boolean searching = false;
private long lastSearchTime = 0;

private String mode = "";
private String rideId = "";

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    requestWindowFeature(Window.FEATURE_NO_TITLE);

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
    startLocationUpdates();
}

private String safe(String value) {

    return value == null
            ? ""
            : value;
}

private void buildScreen() {

    FrameLayout root =
            new FrameLayout(this);

    root.setBackgroundColor(
            Color.WHITE
    );

    // =========================================================
    // MAP
    // =========================================================

    webView =
            new WebView(this);

    FrameLayout.LayoutParams mapParams =
            new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            );

    root.addView(
            webView,
            mapParams
    );

    WebSettings settings =
            webView.getSettings();

    settings.setJavaScriptEnabled(true);
    settings.setDomStorageEnabled(true);
    settings.setDatabaseEnabled(true);
    settings.setBuiltInZoomControls(false);
    settings.setDisplayZoomControls(false);

    webView.setWebViewClient(
            new WebViewClient()
    );

    webView.setFocusable(false);
    webView.setFocusableInTouchMode(false);

    // =========================================================
    // TOP PANEL
    // =========================================================

    LinearLayout topPanel =
            new LinearLayout(this);

    topPanel.setOrientation(
            LinearLayout.VERTICAL
    );

    topPanel.setPadding(
            20,
            20,
            20,
            15
    );

    topPanel.setBackgroundColor(
            Color.WHITE
    );

    topPanel.setClickable(true);
    topPanel.setFocusable(true);
    topPanel.setFocusableInTouchMode(true);
    topPanel.setElevation(50f);

    FrameLayout.LayoutParams topParams =
            new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
            );

    topParams.gravity =
            Gravity.TOP;

    root.addView(
            topPanel,
            topParams
    );

    TextView title =
            new TextView(this);

    title.setText(
            "🗺️ SAKAY NA MAP"
    );

    title.setTextSize(22);
    title.setTextColor(Color.BLACK);
    title.setGravity(Gravity.CENTER);

    title.setPadding(
            5,
            5,
            5,
            10
    );

    topPanel.addView(title);

    locationText =
            new TextView(this);

    locationText.setText(
            "📍 Getting your location..."
    );

    locationText.setTextSize(15);
    locationText.setTextColor(
            Color.DKGRAY
    );

    locationText.setPadding(
            5,
            5,
            5,
            3
    );

    topPanel.addView(
            locationText
    );

    addressText =
            new TextView(this);

    addressText.setText(
            "Address: Finding address..."
    );

    addressText.setTextSize(14);
    addressText.setTextColor(
            Color.DKGRAY
    );

    addressText.setPadding(
            5,
            2,
            5,
            8
    );

    topPanel.addView(
            addressText
    );

    // =========================================================
    // SEARCH
    // =========================================================

    if (!"LIVE_RIDE".equalsIgnoreCase(mode)) {

        TextView searchLabel =
                new TextView(this);

        searchLabel.setText(
                "🔎 SEARCH PLACE / ADDRESS"
        );

        searchLabel.setTextSize(16);
        searchLabel.setTextColor(
                Color.BLACK
        );

        searchLabel.setPadding(
                5,
                5,
                5,
                5
        );

        topPanel.addView(
                searchLabel
        );

        LinearLayout searchRow =
                new LinearLayout(this);

        searchRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        searchRow.setGravity(
                Gravity.CENTER_VERTICAL
        );

        searchRow.setClickable(true);
        searchRow.setFocusable(true);

        topPanel.addView(
                searchRow,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        searchInput =
                new EditText(this);

        searchInput.setHint(
                "Example: Jollibee, SM City Imus"
        );

        searchInput.setTextSize(16);
        searchInput.setSingleLine(true);

        searchInput.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        );

        searchInput.setImeOptions(
                EditorInfo.IME_ACTION_SEARCH
        );

        searchInput.setFocusable(true);
        searchInput.setFocusableInTouchMode(true);
        searchInput.setClickable(true);
        searchInput.setEnabled(true);

        searchInput.setTextColor(
                Color.BLACK
        );

        searchInput.setHintTextColor(
                Color.GRAY
        );

        LinearLayout.LayoutParams inputParams =
                new LinearLayout.LayoutParams(
                        0,
                        60,
                        1
                );

        searchRow.addView(
                searchInput,
                inputParams
        );

        searchButton =
                new Button(this);

        searchButton.setText(
                "SEARCH"
        );

        searchButton.setTextColor(
                Color.WHITE
        );

        searchButton.setBackgroundColor(
                Color.rgb(0, 130, 200)
        );

        searchButton.setClickable(true);
        searchButton.setFocusable(true);
        searchButton.setEnabled(true);
        searchButton.setElevation(60f);

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        125,
                        60
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

        searchButton.setOnClickListener(
                v -> {
                    hideKeyboard();
                    searchPlace();
                }
        );

        searchInput.setOnEditorActionListener(
                (v, actionId, event) -> {

                    if (actionId ==
                            EditorInfo.IME_ACTION_SEARCH) {

                        hideKeyboard();
                        searchPlace();

                        return true;
                    }

                    return false;
                }
        );

        searchRow.bringToFront();
        searchInput.bringToFront();
        searchButton.bringToFront();
        topPanel.bringToFront();
    }

    // =========================================================
    // BOTTOM PANEL
    // =========================================================

    LinearLayout bottomPanel =
            new LinearLayout(this);

    bottomPanel.setOrientation(
            LinearLayout.VERTICAL
    );

    bottomPanel.setPadding(
            15,
            10,
            15,
            15
    );

    bottomPanel.setBackgroundColor(
            Color.WHITE
    );

    bottomPanel.setClickable(true);
    bottomPanel.setFocusable(true);
    bottomPanel.setElevation(50f);

    FrameLayout.LayoutParams bottomParams =
            new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
            );

    bottomParams.gravity =
            Gravity.BOTTOM;

    root.addView(
            bottomPanel,
            bottomParams
    );

    Button currentButton =
            new Button(this);

    currentButton.setText(
            "📍 USE MY CURRENT LOCATION"
    );

    currentButton.setOnClickListener(
            v -> useCurrentLocation()
    );

    bottomPanel.addView(
            currentButton
    );

    if (!"LIVE_RIDE".equalsIgnoreCase(mode)) {

        Button destinationButton =
                new Button(this);

        destinationButton.setText(
                "✅ USE SELECTED DESTINATION"
        );

        destinationButton.setOnClickListener(
                v -> confirmDestination()
        );

        bottomPanel.addView(
                destinationButton
        );
    }

    Button closeButton =
            new Button(this);

    closeButton.setText(
            "✖ CLOSE MAP"
    );

    closeButton.setOnClickListener(
            v -> finish()
    );

    bottomPanel.addView(
            closeButton
    );

    setContentView(root);

    loadMap();

    topPanel.bringToFront();
    bottomPanel.bringToFront();

    if (searchInput != null) {
        searchInput.bringToFront();
    }

    if (searchButton != null) {
        searchButton.bringToFront();
    }
}

// =============================================================
// MAP
// =============================================================

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
            "L.map('map').setView([14.40,120.94],14);" +

            "L.tileLayer(" +
            "'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'," +
            "{" +
            "maxZoom:19," +
            "attribution:'© OpenStreetMap'" +
            "}" +
            ").addTo(map);" +

            "var currentMarker=null;" +
            "var selectedMarker=null;" +

            "function setCurrent(lat,lng){" +

            "if(currentMarker){" +
            "map.removeLayer(currentMarker);" +
            "}" +

            "currentMarker=" +
            "L.marker([lat,lng])" +
            ".addTo(map)" +
            ".bindPopup('📍 You are here')" +
            ".openPopup();" +

            "map.setView([lat,lng],16);" +

            "}" +

            "function setSelected(lat,lng,name){" +

            "if(selectedMarker){" +
            "map.removeLayer(selectedMarker);" +
            "}" +

            "selectedMarker=" +
            "L.marker([lat,lng])" +
            ".addTo(map)" +
            ".bindPopup(name || 'Selected destination')" +
            ".openPopup();" +

            "map.setView([lat,lng],17);" +

            "}" +

            "map.on('click',function(e){" +
            "AndroidMap.onMapTap(" +
            "e.latlng.lat,e.latlng.lng);" +
            "});" +

            "</script>" +

            "</body>" +
            "</html>";

    webView.addJavascriptInterface(
            new MapBridge(),
            "AndroidMap"
    );

    webView.loadDataWithBaseURL(
            "https://sakayna.local/",
            html,
            "text/html",
            "UTF-8",
            null
    );
}

private class MapBridge {

    @JavascriptInterface
    public void onMapTap(
            double lat,
            double lng) {

        runOnUiThread(() -> {

            selectedLat = lat;
            selectedLng = lng;

            selectedPlaceName = "";
            selectedAddress = "";

            showSelectedLocation(
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

// =============================================================
// GPS
// =============================================================

private void startLocationUpdates() {

    locationManager =
            (LocationManager)
                    getSystemService(
                            LOCATION_SERVICE
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

                    currentLocation =
                            location;

                    currentLat =
                            location.getLatitude();

                    currentLng =
                            location.getLongitude();

                    updateCurrentLocationUI(
                            location
                    );

                    if (webView != null) {

                        webView.evaluateJavascript(
                                "setCurrent("
                                        + currentLat
                                        + ","
                                        + currentLng
                                        + ");",
                                null
                        );
                    }

                    reverseCurrentAddress(
                            currentLat,
                            currentLng
                    );
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

        Location last =
                locationManager.getLastKnownLocation(
                        LocationManager.GPS_PROVIDER
                );

        if (last == null) {

            last =
                    locationManager.getLastKnownLocation(
                            LocationManager.NETWORK_PROVIDER
                    );
        }

        if (last != null) {

            currentLocation = last;

            currentLat =
                    last.getLatitude();

            currentLng =
                    last.getLongitude();

            updateCurrentLocationUI(
                    last
            );

            reverseCurrentAddress(
                    currentLat,
                    currentLng
            );
        }

    } catch (SecurityException ignored) {
    }
}

private void updateCurrentLocationUI(
        Location location) {

    locationText.setText(
            "📍 CURRENT LOCATION\n"
                    + String.format(
                    Locale.US,
                    "%.6f, %.6f",
                    location.getLatitude(),
                    location.getLongitude()
            )
    );
}

private void useCurrentLocation() {

    if (currentLocation == null) {

        Toast.makeText(
                this,
                "Getting your location. Please wait.",
                Toast.LENGTH_SHORT
        ).show();

        return;
    }

    selectedLat =
            currentLat;

    selectedLng =
            currentLng;

    selectedPlaceName =
            currentPlaceName;

    selectedAddress =
            currentAddress;

    if (webView != null) {

        webView.evaluateJavascript(
                "setSelected("
                        + selectedLat
                        + ","
                        + selectedLng
                        + ","
                        + JSONObject.quote(
                        selectedPlaceName
                )
                        + ");",
                null
        );
    }

    Toast.makeText(
            this,
            "📍 Current location selected.",
            Toast.LENGTH_SHORT
    ).show();
}

// =============================================================
// DESTINATION SEARCH
// =============================================================

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
                "Enter a place or address.",
                Toast.LENGTH_SHORT
        ).show();

        searchInput.requestFocus();

        return;
    }

    if (searching) {
        return;
    }

    long now =
            System.currentTimeMillis();

    if (now - lastSearchTime < 1000) {
        return;
    }

    lastSearchTime = now;
    searching = true;

    searchButton.setEnabled(false);
    searchButton.setText(
            "SEARCHING"
    );

    hideKeyboard();

    new Thread(() -> {

        boolean found =
                searchWithAndroidGeocoder(
                        query
                );

        if (!found) {

            searchWithNominatim(
                    query
            );
        }

    }).start();
}

/*
 * FIRST SEARCH METHOD
 *
 * Use Android's geocoder first.
 *
 * The old problem was taking result #1.
 * This version checks up to 10 results and
 * selects the result whose name/address matches
 * the user's search and is closest to the passenger.
 */
private boolean searchWithAndroidGeocoder(
        String query) {

    try {

        Geocoder geocoder =
                new Geocoder(
                        this,
                        Locale.getDefault()
                );

        List<Address> results =
                geocoder.getFromLocationName(
                        query,
                        10
                );

        if (results == null
                || results.isEmpty()) {

            return false;
        }

        Address best =
                null;

        double bestScore =
                Double.MAX_VALUE;

        String wanted =
                query.toLowerCase(
                        Locale.US
                );

        for (Address address :
                results) {

            if (address == null) {
                continue;
            }

            double lat =
                    address.getLatitude();

            double lng =
                    address.getLongitude();

            String feature =
                    safe(
                            address.getFeatureName()
                    );

            String thoroughfare =
                    safe(
                            address.getThoroughfare()
                    );

            String locality =
                    safe(
                            address.getLocality()
                    );

            String subLocality =
                    safe(
                            address.getSubLocality()
                    );

            String full =
                    safe(
                            address.getAddressLine(0)
                    );

            String combined =
                    (
                            feature
                                    + " "
                                    + thoroughfare
                                    + " "
                                    + locality
                                    + " "
                                    + subLocality
                                    + " "
                                    + full
                    ).toLowerCase(
                            Locale.US
                    );

            double score =
                    distanceBetweenKm(
                            currentLat,
                            currentLng,
                            lat,
                            lng
                    );

            /*
             * Strong preference for exact searched
             * place text.
             */
            if (feature
                    .toLowerCase(Locale.US)
                    .equals(wanted)) {

                score -= 1000;
            } else if (combined.contains(wanted)) {

                score -= 500;
            }

            /*
             * Prefer actual named places over a generic
             * road/locality result.
             */
            if (!feature.isEmpty()) {
                score -= 30;
            }

            if (!thoroughfare.isEmpty()
                    && feature.isEmpty()) {

                score += 20;
            }

            if (best == null
                    || score < bestScore) {

                best = address;
                bestScore = score;
            }
        }

        if (best == null) {
            return false;
        }

        double lat =
                best.getLatitude();

        double lng =
                best.getLongitude();

        String name =
                safe(
                        best.getFeatureName()
                );

        if (name.isEmpty()) {

            name =
                    safe(
                            best.getPremises()
                    );
        }

        if (name.isEmpty()) {

            name = query;
        }

        String readable =
                safe(
                        best.getAddressLine(0)
                );

        if (readable.isEmpty()) {
            readable = name;
        }

        final String finalName =
                name;

        final String finalAddress =
                readable;

        runOnUiThread(() -> {

            applySearchResult(
                    lat,
                    lng,
                    finalName,
                    finalAddress
            );
        });

        return true;

    } catch (Exception ignored) {

        return false;
    }
}

/*
 * FALLBACK SEARCH
 *
 * If Android Geocoder has no result, use Nominatim.
 */
private void searchWithNominatim(
        String query) {

    HttpURLConnection connection =
            null;

    try {

        String encoded =
                URLEncoder.encode(
                        query + ", Philippines",
                        "UTF-8"
                );

        String urlString;

        if (currentLat != 0.0
                || currentLng != 0.0) {

            double north =
                    currentLat + 0.20;

            double south =
                    currentLat - 0.20;

            double east =
                    currentLng + 0.20;

            double west =
                    currentLng - 0.20;

            urlString =
                    "https://nominatim.openstreetmap.org/search"
                            + "?q="
                            + encoded
                            + "&format=json"
                            + "&limit=10"
                            + "&addressdetails=1"
                            + "&viewbox="
                            + west
                            + ","
                            + north
                            + ","
                            + east
                            + ","
                            + south;
        } else {

            urlString =
                    "https://nominatim.openstreetmap.org/search"
                            + "?q="
                            + encoded
                            + "&format=json"
                            + "&limit=10"
                            + "&addressdetails=1";
        }

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

        connection.setRequestProperty(
                "Accept",
                "application/json"
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

        JSONArray results =
                new JSONArray(
                        response.toString()
                );

        if (results.length() == 0) {

            runOnUiThread(
                    this::searchFailed
            );

            return;
        }

        JSONObject best =
                null;

        double bestScore =
                Double.MAX_VALUE;

        String wanted =
                query.toLowerCase(
                        Locale.US
                );

        for (int i = 0;
             i < results.length();
             i++) {

            JSONObject item =
                    results.getJSONObject(i);

            double lat =
                    Double.parseDouble(
                            item.getString("lat")
                    );

            double lng =
                    Double.parseDouble(
                            item.getString("lon")
                    );

            String display =
                    item.optString(
                            "display_name",
                            ""
                    );

            String name =
                    extractPlaceName(
                            item,
                            query
                    );

            String combined =
                    (
                            name
                                    + " "
                                    + display
                    ).toLowerCase(
                            Locale.US
                    );

            double score =
                    distanceBetweenKm(
                            currentLat,
                            currentLng,
                            lat,
                            lng
                    );

            if (name
                    .toLowerCase(Locale.US)
                    .equals(wanted)) {

                score -= 1000;
            } else if (combined.contains(wanted)) {

                score -= 500;
            }

            String type =
                    item.optString(
                            "type",
                            ""
                    );

            if ("road".equalsIgnoreCase(type)
                    || "residential".equalsIgnoreCase(
                    type)) {

                score += 30;
            }

            if (best == null
                    || score < bestScore) {

                best = item;
                bestScore = score;
            }
        }

        if (best == null) {

            runOnUiThread(
                    this::searchFailed
            );

            return;
        }

        double lat =
                Double.parseDouble(
                        best.getString("lat")
                );

        double lng =
                Double.parseDouble(
                        best.getString("lon")
                );

        String name =
                extractPlaceName(
                        best,
                        query
                );

        String address =
                buildReadableAddress(
                        best,
                        best.optString(
                                "display_name",
                                query
                        )
                );

        final String finalName =
                name;

        final String finalAddress =
                address;

        runOnUiThread(() -> {

            applySearchResult(
                    lat,
                    lng,
                    finalName,
                    finalAddress
            );
        });

    } catch (Exception ignored) {

        runOnUiThread(
                this::searchFailed
        );

    } finally {

        if (connection != null) {
            connection.disconnect();
        }
    }
}

private double distanceBetweenKm(
        double lat1,
        double lng1,
        double lat2,
        double lng2) {

    if ((lat1 == 0.0 && lng1 == 0.0)
            || (lat2 == 0.0 && lng2 == 0.0)) {

        return 0.0;
    }

    float[] result =
            new float[1];

    Location.distanceBetween(
            lat1,
            lng1,
            lat2,
            lng2,
            result
    );

    return result[0] / 1000.0;
}

private String extractPlaceName(
        JSONObject object,
        String fallback) {

    try {

        String directName =
                object.optString(
                        "name",
                        ""
                );

        if (!directName.isEmpty()) {
            return directName;
        }

        JSONObject address =
                object.optJSONObject(
                        "address"
                );

        if (address != null) {

            String amenity =
                    address.optString(
                            "amenity",
                            ""
                    );

            if (!amenity.isEmpty()) {
                return amenity;
            }

            String shop =
                    address.optString(
                            "shop",
                            ""
                    );

            if (!shop.isEmpty()) {
                return shop;
            }

            String building =
                    address.optString(
                            "building",
                            ""
                    );

            if (!building.isEmpty()) {
                return building;
            }
        }

    } catch (Exception ignored) {
    }

    return fallback;
}

private String buildReadableAddress(
        JSONObject object,
        String fallback) {

    try {

        JSONObject address =
                object.optJSONObject(
                        "address"
                );

        if (address != null) {

            String house =
                    address.optString(
                            "house_number",
                            ""
                    );

            String road =
                    address.optString(
                            "road",
                            ""
                    );

            String barangay =
                    address.optString(
                            "barangay",
                            ""
                    );

            if (barangay.isEmpty()) {

                barangay =
                        address.optString(
                                "suburb",
                                ""
                        );
            }

            if (barangay.isEmpty()) {

                barangay =
                        address.optString(
                                "village",
                                ""
                        );
            }

            String city =
                    address.optString(
                            "city",
                            ""
                    );

            if (city.isEmpty()) {

                city =
                        address.optString(
                                "town",
                                ""
                        );
            }

            if (city.isEmpty()) {

                city =
                        address.optString(
                                "municipality",
                                ""
                        );
            }

            String province =
                    address.optString(
                            "state",
                            ""
                    );

            StringBuilder result =
                    new StringBuilder();

            if (!house.isEmpty()) {
                result.append(house);
            }

            if (!road.isEmpty()) {

                if (result.length() > 0) {
                    result.append(" ");
                }

                result.append(road);
            }

            if (!barangay.isEmpty()) {

                if (result.length() > 0) {
                    result.append(", ");
                }

                result.append(barangay);
            }

            if (!city.isEmpty()) {

                if (result.length() > 0) {
                    result.append(", ");
                }

                result.append(city);
            }

            if (!province.isEmpty()) {

                if (result.length() > 0) {
                    result.append(", ");
                }

                result.append(province);
            }

            if (result.length() > 0) {
                return result.toString();
            }
        }

    } catch (Exception ignored) {
    }

    return fallback;
}

private void applySearchResult(
        double lat,
        double lng,
        String name,
        String addressValue) {

    if (name == null
            || name.trim().isEmpty()) {

        name =
                "Selected destination";
    }

    if (addressValue == null
            || addressValue.trim().isEmpty()) {

        addressValue = name;
    }

    selectedLat =
            lat;

    selectedLng =
            lng;

    selectedPlaceName =
            name;

    selectedAddress =
            addressValue;

    final String destinationName =
            name;

    final String destinationAddress =
            addressValue;

    locationText.setText(
            "📍 SELECTED LOCATION\n"
                    + String.format(
                    Locale.US,
                    "%.6f, %.6f",
                    lat,
                    lng
            )
    );

    addressText.setText(
            "🏁 "
                    + destinationName
                    + "\n"
                    + destinationAddress
    );

    if (webView != null) {

        webView.evaluateJavascript(
                "setSelected("
                        + lat
                        + ","
                        + lng
                        + ","
                        + JSONObject.quote(
                        destinationName
                )
                        + ");",
                null
        );
    }

    searching = false;

    if (searchButton != null) {

        searchButton.setEnabled(true);
        searchButton.setText(
                "SEARCH"
        );
    }

    Toast.makeText(
            this,
            "📍 Found: "
                    + destinationName,
            Toast.LENGTH_SHORT
    ).show();
}

private void searchFailed() {

    searching = false;

    if (searchButton != null) {

        searchButton.setEnabled(true);
        searchButton.setText(
                "SEARCH"
        );
    }

    Toast.makeText(
            this,
            "Place not found. Try a more specific name.",
            Toast.LENGTH_LONG
    ).show();
}

// =============================================================
// CURRENT LOCATION ADDRESS
// =============================================================

private void reverseCurrentAddress(
        double lat,
        double lng) {

    new Thread(() -> {

        String result =
                getAddressFromGeocoder(
                        lat,
                        lng
                );

        if (result == null
                || result.isEmpty()) {

            result =
                    getAddressFromNominatim(
                            lat,
                            lng
                    );
        }

        final String finalResult =
                result;

        runOnUiThread(() -> {

            if (finalResult != null
                    && !finalResult.isEmpty()) {

                currentAddress =
                        finalResult;

                currentPlaceName =
                        getShortPlaceName(
                                finalResult
                        );

                /*
                 * IMPORTANT:
                 * Keep the passenger's real address visible.
                 * Do not replace it with latitude/longitude only.
                 */
                addressText.setText(
                        "📍 "
                                + currentPlaceName
                                + "\n"
                                + currentAddress
                );
            }
        });

    }).start();
}

private void reverseGeocode(
        double lat,
        double lng) {

    new Thread(() -> {

        String result =
                getAddressFromGeocoder(
                        lat,
                        lng
                );

        if (result == null
                || result.isEmpty()) {

            result =
                    getAddressFromNominatim(
                            lat,
                            lng
                    );
        }

        final String finalResult =
                result;

        runOnUiThread(() -> {

            if (finalResult != null
                    && !finalResult.isEmpty()) {

                selectedAddress =
                        finalResult;

                if (selectedPlaceName == null
                        || selectedPlaceName.isEmpty()) {

                    selectedPlaceName =
                            getShortPlaceName(
                                    finalResult
                            );
                }

                addressText.setText(
                        "🏁 "
                                + selectedPlaceName
                                + "\n"
                                + finalResult
                );

            } else {

                selectedPlaceName =
                        "Selected destination";

                selectedAddress =
                        "Selected map location";

                addressText.setText(
                        "🏁 Selected destination"
                );
            }
        });

    }).start();
}

private String getAddressFromGeocoder(
        double lat,
        double lng) {

    try {

        Geocoder geocoder =
                new Geocoder(
                        this,
                        Locale.getDefault()
                );

        List<Address> addresses =
                geocoder.getFromLocation(
                        lat,
                        lng,
                        1
                );

        if (addresses != null
                && !addresses.isEmpty()) {

            String value =
                    addresses.get(0)
                            .getAddressLine(0);

            if (value != null
                    && !value.trim().isEmpty()) {

                return value;
            }
        }

    } catch (Exception ignored) {
    }

    return "";
}

private String getAddressFromNominatim(
        double lat,
        double lng) {

    HttpURLConnection connection =
            null;

    try {

        URL url =
                new URL(
                        "https://nominatim.openstreetmap.org/reverse"
                                + "?lat="
                                + lat
                                + "&lon="
                                + lng
                                + "&format=json"
                                + "&zoom=18"
                                + "&addressdetails=1"
                );

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

        StringBuilder response =
                new StringBuilder();

        String line;

        while ((line = reader.readLine())
                != null) {

            response.append(line);
        }

        reader.close();

        JSONObject object =
                new JSONObject(
                        response.toString()
                );

        return object.optString(
                "display_name",
                ""
        );

    } catch (Exception ignored) {

        return "";

    } finally {

        if (connection != null) {
            connection.disconnect();
        }
    }
}

private String getShortPlaceName(
        String address) {

    if (address == null
            || address.trim().isEmpty()) {

        return "Selected location";
    }

    String[] parts =
            address.split(",");

    if (parts.length > 0) {
        return parts[0].trim();
    }

    return address;
}

// =============================================================
// MAP TAP
// =============================================================

private void showSelectedLocation(
        double lat,
        double lng) {

    locationText.setText(
            "📍 SELECTED LOCATION\n"
                    + String.format(
                    Locale.US,
                    "%.6f, %.6f",
                    lat,
                    lng
            )
    );

    addressText.setText(
            "🏁 Finding real address..."
    );

    if (webView != null) {

        webView.evaluateJavascript(
                "setSelected("
                        + lat
                        + ","
                        + lng
                        + ",'Selected destination');",
                null
        );
    }
}

// =============================================================
// CONFIRM DESTINATION
// =============================================================

private void confirmDestination() {

    if (selectedLat == 0.0
            && selectedLng == 0.0) {

        Toast.makeText(
                this,
                "Select a destination first.",
                Toast.LENGTH_LONG
        ).show();

        return;
    }

    Intent result =
            new Intent();

    result.putExtra(
            "destination_latitude",
            selectedLat
    );

    result.putExtra(
            "destination_longitude",
            selectedLng
    );

    result.putExtra(
            "destinationName",
            selectedPlaceName
    );

    result.putExtra(
            "destination_address",
            selectedAddress
    );

    result.putExtra(
            "latitude",
            selectedLat
    );

    result.putExtra(
            "longitude",
            selectedLng
    );

    setResult(
            RESULT_OK,
            result
    );

    finish();
}

// =============================================================
// KEYBOARD
// =============================================================

private void hideKeyboard() {

    try {

        InputMethodManager imm =
                (InputMethodManager)
                        getSystemService(
                                Context.INPUT_METHOD_SERVICE
                        );

        View view =
                getCurrentFocus();

        if (imm != null
                && view != null) {

            imm.hideSoftInputFromWindow(
                    view.getWindowToken(),
                    0
            );
        }

        if (searchInput != null) {
            searchInput.clearFocus();
        }

    } catch (Exception ignored) {
    }
}

// =============================================================
// PERMISSION
// =============================================================

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
                && grantResults[0] ==
                PackageManager.PERMISSION_GRANTED) {

            beginLocationUpdates();

        } else {

            Toast.makeText(
                    this,
                    "Location permission is required for GPS.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }
}

// =============================================================
// DESTROY
// =============================================================

@Override
protected void onDestroy() {

    if (locationManager != null
            && locationListener != null) {

        try {

            locationManager.removeUpdates(
                    locationListener
            );

        } catch (Exception ignored) {
        }
    }

    if (webView != null) {

        webView.stopLoading();

        webView.loadUrl(
                "about:blank"
        );

        webView.destroy();

        webView = null;
    }

    super.onDestroy();
}

}
