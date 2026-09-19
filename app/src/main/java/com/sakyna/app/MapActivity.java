
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

import org.json.JSONArray;
import org.json.JSONObject;

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

private WebView webView;

private TextView locationText;
private TextView addressText;
private EditText searchInput;
private Button searchButton;

private LinearLayout resultsContainer;

private LocationManager locationManager;
private LocationListener locationListener;

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

private String mode = "";
private String rideId = "";

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    requestWindowFeature(Window.FEATURE_NO_TITLE);

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

// ============================================================
// SCREEN
// ============================================================

private void buildScreen() {

    FrameLayout root =
            new FrameLayout(this);

    root.setBackgroundColor(
            Color.WHITE
    );

    // --------------------------------------------------------
    // MAP
    // --------------------------------------------------------

    webView =
            new WebView(this);

    WebView.setWebContentsDebuggingEnabled(false);

    WebSettings settings =
            webView.getSettings();

    settings.setJavaScriptEnabled(true);
    settings.setDomStorageEnabled(true);
    settings.setDatabaseEnabled(true);

    webView.setWebViewClient(
            new WebViewClient()
    );

    webView.setFocusable(false);
    webView.setFocusableInTouchMode(false);

    FrameLayout.LayoutParams mapParams =
            new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            );

    root.addView(
            webView,
            mapParams
    );

    // --------------------------------------------------------
    // TOP PANEL
    // --------------------------------------------------------

    LinearLayout topPanel =
            new LinearLayout(this);

    topPanel.setOrientation(
            LinearLayout.VERTICAL
    );

    topPanel.setPadding(
            18,
            18,
            18,
            12
    );

    topPanel.setBackgroundColor(
            Color.WHITE
    );

    topPanel.setElevation(100f);

    topPanel.setClickable(true);
    topPanel.setFocusable(true);

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
            3,
            5,
            8
    );

    topPanel.addView(title);

    locationText =
            new TextView(this);

    locationText.setText(
            "📍 Getting your real location..."
    );

    locationText.setTextSize(15);
    locationText.setTextColor(Color.DKGRAY);

    locationText.setPadding(
            5,
            3,
            5,
            3
    );

    topPanel.addView(
            locationText
    );

    addressText =
            new TextView(this);

    addressText.setText(
            "Address: Finding your real address..."
    );

    addressText.setTextSize(14);
    addressText.setTextColor(Color.DKGRAY);

    addressText.setPadding(
            5,
            2,
            5,
            8
    );

    topPanel.addView(
            addressText
    );

    // --------------------------------------------------------
    // DESTINATION SEARCH
    // --------------------------------------------------------

    if (!"LIVE_RIDE".equalsIgnoreCase(mode)) {

        TextView searchLabel =
                new TextView(this);

        searchLabel.setText(
                "🔎 SEARCH DESTINATION"
        );

        searchLabel.setTextSize(17);
        searchLabel.setTextColor(Color.BLACK);

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

        searchRow.setBackgroundColor(
                Color.WHITE
        );

        topPanel.addView(
                searchRow,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        65
                )
        );

        searchInput =
                new EditText(this);

        searchInput.setHint(
                "Type destination here"
        );

        searchInput.setTextSize(17);

        searchInput.setTextColor(
                Color.BLACK
        );

        searchInput.setHintTextColor(
                Color.GRAY
        );

        searchInput.setSingleLine(true);

        searchInput.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        );

        searchInput.setImeOptions(
                EditorInfo.IME_ACTION_SEARCH
        );

        searchInput.setPadding(
                15,
                0,
                15,
                0
        );

        searchInput.setBackgroundColor(
                Color.rgb(245, 245, 245)
        );

        searchInput.setFocusable(true);
        searchInput.setFocusableInTouchMode(true);
        searchInput.setClickable(true);
        searchInput.setEnabled(true);

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

        searchButton.setTextSize(14);

        searchButton.setBackgroundColor(
                Color.rgb(0, 130, 200)
        );

        searchButton.setEnabled(true);
        searchButton.setClickable(true);
        searchButton.setFocusable(true);

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        120,
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
                v -> searchPlace()
        );

        searchInput.setOnEditorActionListener(
                (v, actionId, event) -> {

                    if (actionId ==
                            EditorInfo.IME_ACTION_SEARCH) {

                        searchPlace();

                        return true;
                    }

                    return false;
                }
        );

        // ----------------------------------------------------
        // SEARCH RESULTS
        // ----------------------------------------------------

        resultsContainer =
                new LinearLayout(this);

        resultsContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        resultsContainer.setBackgroundColor(
                Color.WHITE
        );

        resultsContainer.setVisibility(
                View.GONE
        );

        LinearLayout.LayoutParams resultsParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        resultsParams.setMargins(
                0,
                5,
                0,
                5
        );

        topPanel.addView(
                resultsContainer,
                resultsParams
        );
    }

    // --------------------------------------------------------
    // BOTTOM PANEL
    // --------------------------------------------------------

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

    bottomPanel.setElevation(100f);

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

// ============================================================
// LEAFLET MAP
// ============================================================

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
            ".bindPopup('📍 Passenger current location')" +
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
            ".bindPopup(name || 'Destination')" +
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

// ============================================================
// GPS
// ============================================================

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
            "📍 PASSENGER LOCATION\n"
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
                "Waiting for your real GPS location...",
                Toast.LENGTH_LONG
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

    if (selectedPlaceName == null
            || selectedPlaceName.trim().isEmpty()) {

        selectedPlaceName =
                "Current location";
    }

    if (selectedAddress == null
            || selectedAddress.trim().isEmpty()) {

        selectedAddress =
                selectedPlaceName;
    }

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

    addressText.setText(
            "📍 "
                    + selectedPlaceName
                    + "\n"
                    + selectedAddress
    );

    Toast.makeText(
            this,
            "Current location selected.",
            Toast.LENGTH_SHORT
    ).show();
}

// ============================================================
// DESTINATION SEARCH
// ============================================================

private void searchPlace() {

    if (searching) {
        return;
    }

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
                "Type a destination first.",
                Toast.LENGTH_SHORT
        ).show();

        searchInput.requestFocus();

        return;
    }

    searching = true;

    hideKeyboard();

    if (searchButton != null) {

        searchButton.setEnabled(false);
        searchButton.setText(
                "SEARCHING..."
        );
    }

    if (resultsContainer != null) {

        resultsContainer.removeAllViews();

        resultsContainer.setVisibility(
                View.VISIBLE
        );

        TextView loading =
                new TextView(this);

        loading.setText(
                "🔎 Searching nearby places..."
        );

        loading.setTextSize(15);
        loading.setTextColor(
                Color.DKGRAY
        );

        loading.setPadding(
                12,
                12,
                12,
                12
        );

        resultsContainer.addView(
                loading
        );
    }

    final String finalQuery =
            query;

    new Thread(() -> {

        List<SearchResult> results =
                searchNominatim(
                        finalQuery
                );

        if (results.isEmpty()) {

            results =
                    searchAndroidGeocoder(
                            finalQuery
                    );
        }

        List<SearchResult> finalResults =
                results;

        runOnUiThread(() -> {

            searching = false;

            if (searchButton != null) {

                searchButton.setEnabled(true);

                searchButton.setText(
                        "SEARCH"
                );
            }

            showSearchResults(
                    finalResults
            );
        });

    }).start();
}

// ============================================================
// NOMINATIM SEARCH
// ============================================================

private List<SearchResult> searchNominatim(
        String query) {

    List<SearchResult> results =
            new ArrayList<>();

    HttpURLConnection connection =
            null;

    try {

        String encoded =
                URLEncoder.encode(
                        query + ", Philippines",
                        "UTF-8"
                );

        String urlText =
                "https://nominatim.openstreetmap.org/search"
                        + "?q="
                        + encoded
                        + "&format=json"
                        + "&addressdetails=1"
                        + "&limit=8";

        if (currentLocation != null) {

            double delta = 0.20;

            double left =
                    currentLng - delta;

            double right =
                    currentLng + delta;

            double top =
                    currentLat + delta;

            double bottom =
                    currentLat - delta;

            urlText +=
                    "&viewbox="
                            + left
                            + ","
                            + top
                            + ","
                            + right
                            + ","
                            + bottom;
        }

        URL url =
                new URL(
                        urlText
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

        while ((line =
                reader.readLine()) != null) {

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

            JSONObject object =
                    array.getJSONObject(i);

            double lat =
                    Double.parseDouble(
                            object.getString(
                                    "lat"
                            )
                    );

            double lng =
                    Double.parseDouble(
                            object.getString(
                                    "lon"
                            )
                    );

            String name =
                    extractPlaceName(
                            object,
                            query
                    );

            String address =
                    buildReadableAddress(
                            object,
                            object.optString(
                                    "display_name",
                                    query
                            )
                    );

            double distance =
                    distanceBetweenKm(
                            currentLat,
                            currentLng,
                            lat,
                            lng
                    );

            results.add(
                    new SearchResult(
                            lat,
                            lng,
                            name,
                            address,
                            distance
                    )
            );
        }

    } catch (Exception ignored) {

    } finally {

        if (connection != null) {
            connection.disconnect();
        }
    }

    return results;
}

// ============================================================
// ANDROID GEOCODER FALLBACK
// ============================================================

private List<SearchResult> searchAndroidGeocoder(
        String query) {

    List<SearchResult> results =
            new ArrayList<>();

    try {

        Geocoder geocoder =
                new Geocoder(
                        this,
                        Locale.getDefault()
                );

        List<Address> addresses =
                geocoder.getFromLocationName(
                        query,
                        8
                );

        if (addresses == null) {
            return results;
        }

        for (Address address :
                addresses) {

            if (!address.hasLatitude()
                    || !address.hasLongitude()) {
                continue;
            }

            double lat =
                    address.getLatitude();

            double lng =
                    address.getLongitude();

            String readable =
                    address.getAddressLine(0);

            if (readable == null
                    || readable.trim().isEmpty()) {

                readable =
                        query;
            }

            String name =
                    readable;

            if (address.getFeatureName() != null
                    && !address.getFeatureName()
                    .trim()
                    .isEmpty()) {

                name =
                        address.getFeatureName();
            }

            double distance =
                    distanceBetweenKm(
                            currentLat,
                            currentLng,
                            lat,
                            lng
                    );

            results.add(
                    new SearchResult(
                            lat,
                            lng,
                            name,
                            readable,
                            distance
                    )
            );
        }

    } catch (Exception ignored) {
    }

    return results;
}

// ============================================================
// SEARCH RESULT UI
// ============================================================

private void showSearchResults(
        List<SearchResult> results) {

    if (resultsContainer == null) {
        return;
    }

    resultsContainer.removeAllViews();

    resultsContainer.setVisibility(
            View.VISIBLE
    );

    if (results == null
            || results.isEmpty()) {

        TextView none =
                new TextView(this);

        none.setText(
                "No matching place found.\n"
                        + "Try a more specific name or include the barangay/city."
        );

        none.setTextSize(15);
        none.setTextColor(
                Color.DKGRAY
        );

        none.setPadding(
                12,
                12,
                12,
                12
        );

        resultsContainer.addView(
                none
        );

        return;
    }

    int limit =
            Math.min(
                    results.size(),
                    6
            );

    for (int i = 0;
         i < limit;
         i++) {

        SearchResult result =
                results.get(i);

        addSearchResultCard(
                result
        );
    }
}

private void addSearchResultCard(
        SearchResult result) {

    LinearLayout card =
            new LinearLayout(this);

    card.setOrientation(
            LinearLayout.VERTICAL
    );

    card.setPadding(
            14,
            10,
            14,
            10
    );

    card.setBackgroundColor(
            Color.rgb(245, 245, 245)
    );

    LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

    params.setMargins(
            0,
            3,
            0,
            3
    );

    resultsContainer.addView(
            card,
            params
    );

    TextView name =
            new TextView(this);

    name.setText(
            "📍 "
                    + result.name
    );

    name.setTextSize(17);
    name.setTextColor(
            Color.BLACK
    );

    name.setPadding(
            0,
            2,
            0,
            4
    );

    card.addView(
            name
    );

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
            0,
            0,
            5
    );

    card.addView(
            address
    );

    card.setClickable(true);
    card.setFocusable(true);

    card.setOnClickListener(
            v -> selectSearchResult(
                    result
            )
    );
}

private void selectSearchResult(
        SearchResult result) {

    selectedLat =
            result.lat;

    selectedLng =
            result.lng;

    selectedPlaceName =
            result.name;

    selectedAddress =
            result.address;

    if (selectedPlaceName == null
            || selectedPlaceName.trim().isEmpty()) {

        selectedPlaceName =
                "Selected destination";
    }

    if (selectedAddress == null
            || selectedAddress.trim().isEmpty()) {

        selectedAddress =
                selectedPlaceName;
    }

    addressText.setText(
            "🏁 "
                    + selectedPlaceName
                    + "\n"
                    + selectedAddress
    );

    locationText.setText(
            "📍 DESTINATION COORDINATES\n"
                    + String.format(
                    Locale.US,
                    "%.6f, %.6f",
                    selectedLat,
                    selectedLng
            )
    );

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

    resultsContainer.setVisibility(
            View.GONE
    );

    Toast.makeText(
            this,
            "✅ Selected: "
                    + selectedPlaceName,
            Toast.LENGTH_SHORT
    ).show();
}

// ============================================================
// PLACE NAME
// ============================================================

private String extractPlaceName(
        JSONObject object,
        String fallback) {

    try {

        String name =
                object.optString(
                        "name",
                        ""
                );

        if (!name.isEmpty()) {
            return name;
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

            String road =
                    address.optString(
                            "road",
                            ""
                    );

            if (!road.isEmpty()) {
                return road;
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

        if (address == null) {
            return fallback;
        }

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

    } catch (Exception ignored) {
    }

    return fallback;
}

// ============================================================
// REVERSE GEOCODING — PASSENGER CURRENT LOCATION
// ============================================================

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
                                + selectedAddress
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

        while ((line =
                reader.readLine()) != null) {

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

        return "Current location";
    }

    String[] parts =
            address.split(",");

    if (parts.length > 0) {

        return parts[0].trim();
    }

    return address;
}

// ============================================================
// MAP TAP
// ============================================================

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

// ============================================================
// CONFIRM DESTINATION
// ============================================================

private void confirmDestination() {

    if (selectedLat == 0.0
            && selectedLng == 0.0) {

        Toast.makeText(
                this,
                "Search and select a destination first.",
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

// ============================================================
// DISTANCE
// ============================================================

private double distanceBetweenKm(
        double lat1,
        double lng1,
        double lat2,
        double lng2) {

    if ((lat1 == 0.0
            && lng1 == 0.0)
            || (lat2 == 0.0
            && lng2 == 0.0)) {

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

// ============================================================
// KEYBOARD
// ============================================================

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

    } catch (Exception ignored) {
    }
}

// ============================================================
// SEARCH RESULT CLASS
// ============================================================

private static class SearchResult {

    double lat;
    double lng;

    String name;
    String address;

    double distanceKm;

    SearchResult(
            double lat,
            double lng,
            String name,
            String address,
            double distanceKm) {

        this.lat = lat;
        this.lng = lng;

        this.name =
                name == null
                        ? "Selected place"
                        : name;

        this.address =
                address == null
                        ? ""
                        : address;

        this.distanceKm =
                distanceKm;
    }
}

// ============================================================
// PERMISSION
// ============================================================

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

// ============================================================
// DESTROY
// ============================================================

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
