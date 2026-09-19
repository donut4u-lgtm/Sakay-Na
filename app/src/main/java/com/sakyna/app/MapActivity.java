
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
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Locale;

public class MapActivity extends Activity {

    private static final int LOCATION_PERMISSION = 1001;

    private WebView webView;
    private EditText searchInput;
    private LinearLayout searchResultsContainer;
    private TextView statusText;
    private TextView destinationText;
    private Button destinationButton;

    private LocationManager locationManager;
    private LocationListener locationListener;

    private FirebaseFirestore db;
    private ListenerRegistration rideListener;
    private ListenerRegistration driverListener;

    private String mode = "SELECT_DESTINATION";
    private String rideId = "";

    private double currentLatitude = 14.4297;
    private double currentLongitude = 120.9367;

    private double destinationLatitude = 0;
    private double destinationLongitude = 0;
    private String destinationAddress = "";
    private boolean destinationChosen = false;

    private boolean mapReady = false;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();

        mode = getIntent().getStringExtra("mode");

        if (mode == null || mode.trim().isEmpty()) {
            mode = "SELECT_DESTINATION";
        }

        rideId = getIntent().getStringExtra("ride_id");

        if (rideId == null || rideId.trim().isEmpty()) {
            rideId = getIntent().getStringExtra("rideId");
        }

        buildScreen();
        startLocation();

        if ("LIVE_RIDE".equals(mode)) {
            setupLiveRide();
        }
    }

    private void buildScreen() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);

        if ("LIVE_RIDE".equals(mode)) {
            title.setText("🛺 SAKAY NA — LIVE RIDE");
        } else {
            title.setText("📍 CHOOSE DESTINATION");
        }

        title.setTextSize(23);
        title.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );
        title.setTextColor(Color.rgb(0, 120, 70));
        title.setGravity(Gravity.CENTER);
        title.setPadding(10, 18, 10, 14);

        root.addView(title);

        if ("SELECT_DESTINATION".equals(mode)) {
            buildSearchArea(root);
        }

        statusText = new TextView(this);
        statusText.setText("📍 Getting your location...");
        statusText.setTextSize(14);
        statusText.setTextColor(Color.DKGRAY);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(10, 6, 10, 8);

        root.addView(statusText);

        destinationText = new TextView(this);
        destinationText.setText(
                "Tap the map or search for a place."
        );
        destinationText.setTextSize(15);
        destinationText.setTextColor(Color.DKGRAY);
        destinationText.setPadding(15, 8, 15, 8);

        root.addView(destinationText);

        webView = new WebView(this);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        webView.setBackgroundColor(Color.WHITE);

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(
                    WebView view,
                    String url
            ) {
                super.onPageFinished(view, url);

                mapReady = true;

                sendCurrentLocationToMap();

                statusText.setText(
                        "📍 Map ready. Getting GPS location..."
                );
            }
        });

        root.addView(
                webView,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        if ("SELECT_DESTINATION".equals(mode)) {

            destinationButton = new Button(this);
            destinationButton.setText(
                    "✅ USE THIS DESTINATION"
            );
            destinationButton.setTextSize(16);
            destinationButton.setTextColor(Color.WHITE);
            destinationButton.setBackgroundColor(
                    Color.rgb(0, 120, 70)
            );
            destinationButton.setEnabled(false);

            destinationButton.setOnClickListener(
                    v -> returnDestination()
            );

            root.addView(destinationButton);

            Button cancelButton = new Button(this);
            cancelButton.setText("BACK");
            cancelButton.setOnClickListener(
                    v -> finish()
            );

            root.addView(cancelButton);

        } else {

            Button backButton = new Button(this);
            backButton.setText("BACK");
            backButton.setOnClickListener(
                    v -> finish()
            );

            root.addView(backButton);
        }

        setContentView(root);

        loadMap();
    }

    private void buildSearchArea(LinearLayout root) {

        LinearLayout searchRow =
                new LinearLayout(this);

        searchRow.setOrientation(
                LinearLayout.HORIZONTAL
        );
        searchRow.setPadding(8, 4, 8, 4);

        searchInput = new EditText(this);
        searchInput.setHint(
                "Search Jollibee, SM, barangay, street..."
        );
        searchInput.setTextSize(16);
        searchInput.setSingleLine(true);

        searchRow.addView(
                searchInput,
                new LinearLayout.LayoutParams(
                        0,
                        -2,
                        1
                )
        );

        Button searchButton = new Button(this);
        searchButton.setText("SEARCH");
        searchButton.setTextSize(13);

        searchButton.setOnClickListener(
                v -> searchPlace()
        );

        searchRow.addView(searchButton);

        root.addView(searchRow);

        ScrollView resultsScroll =
                new ScrollView(this);

        searchResultsContainer =
                new LinearLayout(this);

        searchResultsContainer.setOrientation(
                LinearLayout.VERTICAL
        );
        searchResultsContainer.setPadding(
                8,
                3,
                8,
                3
        );

        resultsScroll.addView(
                searchResultsContainer
        );

        root.addView(
                resultsScroll,
                new LinearLayout.LayoutParams(
                        -1,
                        150
                )
        );
    }

    private void loadMap() {

        String html =
                "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta name='viewport' " +
                "content='width=device-width, " +
                "initial-scale=1.0'>" +

                "<link rel='stylesheet' " +
                "href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'>" +

                "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'>" +
                "</script>" +

                "<style>" +
                "html,body,#map{" +
                "height:100%;" +
                "margin:0;" +
                "padding:0;" +
                "}" +
                "</style>" +

                "</head>" +
                "<body>" +

                "<div id='map'></div>" +

                "<script>" +

                "var map=L.map('map').setView([" +
                currentLatitude +
                "," +
                currentLongitude +
                "],14);" +

                "L.tileLayer(" +
                "'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'," +
                "{maxZoom:19," +
                "attribution:'© OpenStreetMap contributors'}" +
                ").addTo(map);" +

                "var userMarker=null;" +
                "var destinationMarker=null;" +
                "var driverMarker=null;" +

                "function setUser(lat,lng){" +

                "if(userMarker==null){" +

                "userMarker=L.marker([" +
                "lat,lng" +
                "]).addTo(map)" +
                ".bindPopup('You are here');" +

                "}else{" +

                "userMarker.setLatLng([" +
                "lat,lng" +
                "]);" +

                "}" +

                "map.setView([lat,lng],16);" +

                "}" +

                "function setDestination(lat,lng,name){" +

                "if(destinationMarker!=null){" +
                "map.removeLayer(destinationMarker);" +
                "}" +

                "destinationMarker=" +
                "L.marker([lat,lng])" +
                ".addTo(map)" +
                ".bindPopup(name)" +
                ".openPopup();" +

                "map.setView([lat,lng],16);" +

                "}" +

                "function setDriver(lat,lng){" +

                "if(driverMarker==null){" +

                "driverMarker=L.marker([lat,lng])" +
                ".addTo(map)" +
                ".bindPopup('Driver');" +

                "}else{" +

                "driverMarker.setLatLng([lat,lng]);" +

                "}" +

                "}" +

                "map.on('click',function(e){" +

                "AndroidMap.tap(" +
                "e.latlng.lat," +
                "e.latlng.lng" +
                ");" +

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

        @android.webkit.JavascriptInterface
        public void tap(
                double lat,
                double lng
        ) {

            runOnUiThread(() -> {

                if ("SELECT_DESTINATION".equals(mode)) {
                    reverseGeocode(lat, lng);
                }
            });
        }
    }

    private void startLocation() {

        locationManager =
                (LocationManager)
                        getSystemService(
                                LOCATION_SERVICE
                        );

        if (checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION
            );

            return;
        }

        locationListener =
                new LocationListener() {

            @Override
            public void onLocationChanged(
                    Location location
            ) {

                if (location == null) {
                    return;
                }

                currentLatitude =
                        location.getLatitude();

                currentLongitude =
                        location.getLongitude();

                sendCurrentLocationToMap();

                statusText.setText(
                        "📍 GPS location found\n" +
                        String.format(
                                Locale.US,
                                "%.6f, %.6f",
                                currentLatitude,
                                currentLongitude
                        )
                );
            }
        };

        try {

            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000,
                    2,
                    locationListener
            );

            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    2000,
                    2,
                    locationListener
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

                currentLatitude =
                        last.getLatitude();

                currentLongitude =
                        last.getLongitude();

                sendCurrentLocationToMap();
            }

        } catch (SecurityException e) {

            statusText.setText(
                    "GPS permission is required."
            );

        } catch (Exception e) {

            statusText.setText(
                    "Unable to start GPS."
            );
        }
    }

    private void sendCurrentLocationToMap() {

        if (!mapReady || webView == null) {
            return;
        }

        sendJS(
                "setUser(" +
                currentLatitude +
                "," +
                currentLongitude +
                ");"
        );
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == LOCATION_PERMISSION) {

            if (grantResults.length > 0
                    && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED) {

                startLocation();

            } else {

                statusText.setText(
                        "Location permission is needed for GPS."
                );
            }
        }
    }

    private void searchPlace() {

        if (!"SELECT_DESTINATION".equals(mode)) {
            return;
        }

        String query =
                searchInput.getText()
                        .toString()
                        .trim();

        if (query.isEmpty()) {

            Toast.makeText(
                    this,
                    "Enter a place name.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        searchResultsContainer.removeAllViews();

        TextView loading =
                new TextView(this);

        loading.setText(
                "🔎 Searching real place names..."
        );
        loading.setTextSize(15);
        loading.setTextColor(Color.DKGRAY);
        loading.setPadding(
                10,
                10,
                10,
                10
        );

        searchResultsContainer.addView(
                loading
        );

        statusText.setText(
                "Searching for " + query + "..."
        );

        new Thread(() -> {

            HttpURLConnection connection = null;

            try {

                String encoded =
                        URLEncoder.encode(
                                query,
                                "UTF-8"
                        );

                String urlString =
                        "https://photon.komoot.io/api/?" +
                        "q=" +
                        encoded +
                        "&limit=8" +
                        "&lat=" +
                        currentLatitude +
                        "&lon=" +
                        currentLongitude +
                        "&zoom=14";

                URL url =
                        new URL(urlString);

                connection =
                        (HttpURLConnection)
                                url.openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                connection.setRequestProperty(
                        "User-Agent",
                        "SakayNa/1.0 Android"
                );

                connection.setRequestProperty(
                        "Accept",
                        "application/json"
                );

                int response =
                        connection.getResponseCode();

                if (response != 200) {
                    throw new Exception(
                            "Search server returned " +
                            response
                    );
                }

                InputStream input =
                        connection.getInputStream();

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        input,
                                        "UTF-8"
                                )
                        );

                StringBuilder result =
                        new StringBuilder();

                String line;

                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                reader.close();
                input.close();

                JSONArray features =
                        new JSONObject(
                                result.toString()
                        ).optJSONArray(
                                "features"
                        );

                runOnUiThread(
                        () -> showPhotonResults(
                                features
                        )
                );

            } catch (Exception e) {

                runOnUiThread(() -> {

                    searchResultsContainer
                            .removeAllViews();

                    TextView error =
                            new TextView(this);

                    error.setText(
                            "Search failed.\n" +
                            "Try: Jollibee Imus\n" +
                            "SM City Imus\n" +
                            "Barangay Buhay na Tubig"
                    );

                    error.setTextSize(14);
                    error.setTextColor(Color.RED);
                    error.setPadding(
                            10,
                            10,
                            10,
                            10
                    );

                    searchResultsContainer.addView(
                            error
                    );

                    statusText.setText(
                            "Search unavailable."
                    );
                });

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }

    private void showPhotonResults(
            JSONArray features
    ) {

        searchResultsContainer.removeAllViews();

        if (features == null
                || features.length() == 0) {

            TextView empty =
                    new TextView(this);

            empty.setText(
                    "No place found.\n" +
                    "Try adding the city or barangay."
            );

            empty.setTextSize(15);
            empty.setTextColor(Color.DKGRAY);
            empty.setPadding(
                    10,
                    12,
                    10,
                    12
            );

            searchResultsContainer.addView(
                    empty
            );

            statusText.setText(
                    "No place found."
            );

            return;
        }

        statusText.setText(
                "Select the correct real place:"
        );

        for (int i = 0;
             i < features.length();
             i++) {

            try {

                JSONObject feature =
                        features.getJSONObject(i);

                JSONArray coordinates =
                        feature
                                .getJSONObject(
                                        "geometry"
                                )
                                .getJSONArray(
                                        "coordinates"
                                );

                double lng =
                        coordinates.getDouble(0);

                double lat =
                        coordinates.getDouble(1);

                JSONObject properties =
                        feature.optJSONObject(
                                "properties"
                        );

                String name =
                        properties == null
                                ? ""
                                : properties.optString(
                                        "name",
                                        ""
                                );

                String houseNumber =
                        properties == null
                                ? ""
                                : properties.optString(
                                        "housenumber",
                                        ""
                                );

                String street =
                        properties == null
                                ? ""
                                : properties.optString(
                                        "street",
                                        ""
                                );

                String district =
                        properties == null
                                ? ""
                                : properties.optString(
                                        "district",
                                        ""
                                );

                String city =
                        properties == null
                                ? ""
                                : properties.optString(
                                        "city",
                                        ""
                                );

                String state =
                        properties == null
                                ? ""
                                : properties.optString(
                                        "state",
                                        ""
                                );

                String display =
                        buildPlaceName(
                                name,
                                houseNumber,
                                street,
                                district,
                                city,
                                state
                        );

                if (display.isEmpty()) {
                    display =
                            String.format(
                                    Locale.US,
                                    "%.6f, %.6f",
                                    lat,
                                    lng
                            );
                }

                Button resultButton =
                        new Button(this);

                resultButton.setText(
                        "📍 " + display
                );

                resultButton.setTextSize(14);
                resultButton.setGravity(
                        Gravity.LEFT |
                        Gravity.CENTER_VERTICAL
                );

                final double selectedLat = lat;
                final double selectedLng = lng;
                final String selectedName =
                        display;

                resultButton.setOnClickListener(
                        v -> selectSearchResult(
                                selectedLat,
                                selectedLng,
                                selectedName
                        )
                );

                searchResultsContainer.addView(
                        resultButton,
                        new LinearLayout.LayoutParams(
                                -1,
                                -2
                        )
                );

            } catch (Exception ignored) {
            }
        }
    }

    private String buildPlaceName(
            String name,
            String houseNumber,
            String street,
            String district,
            String city,
            String state
    ) {

        StringBuilder result =
                new StringBuilder();

        addPart(result, name);
        addPart(result, houseNumber);
        addPart(result, street);
        addPart(result, district);
        addPart(result, city);

        if (result.length() == 0) {
            addPart(result, state);
        }

        return result.toString();
    }

    private void addPart(
            StringBuilder builder,
            String value
    ) {

        if (value == null) {
            return;
        }

        value = value.trim();

        if (value.isEmpty()) {
            return;
        }

        if (builder.toString().contains(value)) {
            return;
        }

        if (builder.length() > 0) {
            builder.append(", ");
        }

        builder.append(value);
    }

    private void selectSearchResult(
            double lat,
            double lng,
            String name
    ) {

        destinationLatitude = lat;
        destinationLongitude = lng;
        destinationAddress = name;
        destinationChosen = true;

        destinationText.setText(
                "🎯 Destination:\n" +
                destinationAddress
        );

        destinationText.setTextColor(
                Color.rgb(0, 110, 70)
        );

        statusText.setText(
                "✅ Destination selected"
        );

        if (destinationButton != null) {
            destinationButton.setEnabled(true);
        }

        if (searchResultsContainer != null) {
            searchResultsContainer.removeAllViews();
        }

        if (searchInput != null) {
            searchInput.setText(name);
        }

        sendJS(
                "setDestination(" +
                lat +
                "," +
                lng +
                ",'" +
                escapeJS(name) +
                "');"
        );

        Toast.makeText(
                this,
                "Destination selected.",
                Toast.LENGTH_SHORT
        ).show();
    }

    /*
     * OpenStreetMap Nominatim reverse geocoding.
     * This replaces Android Geocoder for map taps.
     */
    private void reverseGeocode(
            double lat,
            double lng
    ) {

        statusText.setText(
                "📍 Finding the real address..."
        );

        new Thread(() -> {

            HttpURLConnection connection = null;

            try {

                String urlString =
                        "https://nominatim.openstreetmap.org/reverse" +
                        "?format=jsonv2" +
                        "&lat=" + lat +
                        "&lon=" + lng +
                        "&zoom=18" +
                        "&addressdetails=1" +
                        "&namedetails=1" +
                        "&accept-language=en";

                URL url =
                        new URL(urlString);

                connection =
                        (HttpURLConnection)
                                url.openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                connection.setRequestProperty(
                        "User-Agent",
                        "SakayNa/1.0 Android"
                );

                connection.setRequestProperty(
                        "Accept",
                        "application/json"
                );

                int response =
                        connection.getResponseCode();

                if (response != 200) {
                    throw new Exception(
                            "Reverse geocoder returned " +
                            response
                    );
                }

                InputStream input =
                        connection.getInputStream();

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        input,
                                        "UTF-8"
                                )
                        );

                StringBuilder result =
                        new StringBuilder();

                String line;

                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                reader.close();
                input.close();

                JSONObject object =
                        new JSONObject(
                                result.toString()
                        );

                JSONObject address =
                        object.optJSONObject(
                                "address"
                        );

                String placeName =
                        object.optString(
                                "name",
                                ""
                        );

                String displayName =
                        object.optString(
                                "display_name",
                                ""
                        );

                String houseNumber =
                        address == null
                                ? ""
                                : address.optString(
                                        "house_number",
                                        ""
                                );

                String road =
                        address == null
                                ? ""
                                : address.optString(
                                        "road",
                                        ""
                                );

                String neighbourhood =
                        address == null
                                ? ""
                                : address.optString(
                                        "neighbourhood",
                                        ""
                                );

                String suburb =
                        address == null
                                ? ""
                                : address.optString(
                                        "suburb",
                                        ""
                                );

                String village =
                        address == null
                                ? ""
                                : address.optString(
                                        "village",
                                        ""
                                );

                String town =
                        address == null
                                ? ""
                                : address.optString(
                                        "town",
                                        ""
                                );

                String city =
                        address == null
                                ? ""
                                : address.optString(
                                        "city",
                                        ""
                                );

                String municipality =
                        address == null
                                ? ""
                                : address.optString(
                                        "municipality",
                                        ""
                                );

                String province =
                        address == null
                                ? ""
                                : address.optString(
                                        "state",
                                        ""
                                );

                String postcode =
                        address == null
                                ? ""
                                : address.optString(
                                        "postcode",
                                        ""
                                );

                String realName =
                        buildReverseAddress(
                                placeName,
                                houseNumber,
                                road,
                                neighbourhood,
                                suburb,
                                village,
                                town,
                                city,
                                municipality,
                                province,
                                postcode
                        );

                if (realName.isEmpty()) {
                    realName = displayName;
                }

                if (realName == null
                        || realName.trim().isEmpty()) {
                    realName =
                            "Selected location";
                }

                final String finalName =
                        realName.trim();

                runOnUiThread(() ->
                        selectSearchResult(
                                lat,
                                lng,
                                finalName
                        )
                );

            } catch (Exception e) {

                runOnUiThread(() -> {

                    statusText.setText(
                            "Address lookup failed. Location is still correct."
                    );

                    /*
                     * Coordinates remain valid even if
                     * the address service fails.
                     */
                    selectSearchResult(
                            lat,
                            lng,
                            String.format(
                                    Locale.US,
                                    "Selected location (%.6f, %.6f)",
                                    lat,
                                    lng
                            )
                    );
                });

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }

    private String buildReverseAddress(
            String name,
            String houseNumber,
            String road,
            String neighbourhood,
            String suburb,
            String village,
            String town,
            String city,
            String municipality,
            String province,
            String postcode
    ) {

        StringBuilder result =
                new StringBuilder();

        /*
         * POI name first.
         * Example:
         * Jollibee
         * Jollibee, Aguinaldo Highway,
         * Imus, Cavite
         */
        addPart(result, name);

        if (!houseNumber.isEmpty()
                && !road.isEmpty()) {

            addPart(
                    result,
                    houseNumber + " " + road
            );

        } else {

            addPart(result, houseNumber);
            addPart(result, road);
        }

        addPart(result, neighbourhood);
        addPart(result, suburb);

        if (!village.isEmpty()) {
            addPart(result, village);
        }

        if (!town.isEmpty()) {
            addPart(result, town);
        }

        if (!city.isEmpty()) {
            addPart(result, city);
        }

        if (!municipality.isEmpty()) {
            addPart(result, municipality);
        }

        addPart(result, province);
        addPart(result, postcode);

        return result.toString();
    }

    private String escapeJS(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", " ")
                .replace("\r", " ");
    }

    private void returnDestination() {

        if (!destinationChosen) {

            Toast.makeText(
                    this,
                    "Search or tap a destination first.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Intent result = new Intent();

        result.putExtra(
                "destination_latitude",
                destinationLatitude
        );

        result.putExtra(
                "destination_longitude",
                destinationLongitude
        );

        result.putExtra(
                "destination_address",
                destinationAddress
        );

        result.putExtra(
                "destinationName",
                destinationAddress
        );

        setResult(
                RESULT_OK,
                result
        );

        finish();
    }

    private void setupLiveRide() {

        if (rideId == null
                || rideId.trim().isEmpty()) {

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

                                        statusText.setText(
                                                "Ride unavailable."
                                        );

                                        return;
                                    }

                                    updateLiveRide(
                                            snapshot
                                    );
                                }
                        );
    }

    private void updateLiveRide(
            DocumentSnapshot ride
    ) {

        String status =
                value(ride, "status");

        String pickupName =
                value(ride, "pickupName");

        String destinationName =
                value(
                        ride,
                        "destinationName"
                );

        if (pickupName.isEmpty()) {
            pickupName =
                    value(
                            ride,
                            "pickup"
                    );
        }

        if (destinationName.isEmpty()) {
            destinationName =
                    value(
                            ride,
                            "destination"
                    );
        }

        statusText.setText(
                "Ride status: " +
                status +
                "\nPickup: " +
                pickupName +
                "\nDestination: " +
                destinationName
        );

        String driverId =
                value(
                        ride,
                        "driverId"
                );

        if (!driverId.isEmpty()) {
            listenToDriver(driverId);
        }
    }

    private void listenToDriver(
            String driverId
    ) {

        if (driverListener != null) {
            driverListener.remove();
            driverListener = null;
        }

        driverListener =
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
                                    snapshot.getDouble(
                                            "latitude"
                                    );

                            Double lng =
                                    snapshot.getDouble(
                                            "longitude"
                                    );

                            if (lat == null) {
                                lat =
                                        snapshot.getDouble(
                                                "lat"
                                        );
                            }

                            if (lng == null) {
                                lng =
                                        snapshot.getDouble(
                                                "lng"
                                        );
                            }

                            if (lat != null
                                    && lng != null) {

                                sendJS(
                                        "setDriver(" +
                                        lat +
                                        "," +
                                        lng +
                                        ");"
                                );
                            }
                        }
                );
    }

    private String value(
            DocumentSnapshot document,
            String field
    ) {

        String value =
                document.getString(field);

        return value == null
                ? ""
                : value.trim();
    }

    private void sendJS(
            String javascript
    ) {

        if (webView == null
                || !mapReady) {
            return;
        }

        runOnUiThread(() ->
                webView.evaluateJavascript(
                        javascript,
                        null
                )
        );
    }

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

        if (rideListener != null) {
            rideListener.remove();
        }

        if (driverListener != null) {
            driverListener.remove();
        }

        super.onDestroy();
    }
}
