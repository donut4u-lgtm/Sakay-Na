
package com.sakyna.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.util.List;
import java.util.Locale;

public class MapActivity extends Activity {

    private static final int LOCATION_PERMISSION_REQUEST = 5001;

    private WebView webView;
    private TextView destinationText;
    private TextView statusText;
    private LinearLayout searchResultsContainer;

    private EditText searchInput;
    private Button searchButton;
    private Button gpsButton;
    private Button destinationButton;
    private Button liveMapButton;
    private Button backButton;

    private LocationManager locationManager;
    private LocationListener locationListener;

    private FirebaseFirestore db;
    private ListenerRegistration rideListener;
    private ListenerRegistration driverListener;

    private String mode = "SELECT_DESTINATION";
    private String rideId = "";

    private double pickupLatitude = 0;
    private double pickupLongitude = 0;

    private double destinationLatitude = 0;
    private double destinationLongitude = 0;

    private double driverLatitude = 0;
    private double driverLongitude = 0;

    private String pickupAddress = "";
    private String destinationAddress = "";

    private boolean mapReady = false;
    private boolean destinationChosen = false;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();

        readIntent();
        buildInterface();
        setupMap();
        requestLocation();
    }

    private void readIntent() {

        Intent intent = getIntent();

        if (intent == null) {
            return;
        }

        String incomingMode =
                intent.getStringExtra("mode");

        if (incomingMode != null &&
                !incomingMode.trim().isEmpty()) {

            mode = incomingMode;
        }

        rideId =
                safe(intent.getStringExtra("ride_id"));

        pickupLatitude =
                intent.getDoubleExtra(
                        "pickup_latitude",
                        0);

        pickupLongitude =
                intent.getDoubleExtra(
                        "pickup_longitude",
                        0);

        destinationLatitude =
                intent.getDoubleExtra(
                        "destination_latitude",
                        0);

        destinationLongitude =
                intent.getDoubleExtra(
                        "destination_longitude",
                        0);

        pickupAddress =
                safe(
                        intent.getStringExtra(
                                "pickup_address"));

        destinationAddress =
                safe(
                        intent.getStringExtra(
                                "destination_address"));

        destinationChosen =
                destinationLatitude != 0 &&
                destinationLongitude != 0;
    }

    private String safe(String value) {

        return value == null ? "" : value;
    }

    private void buildInterface() {

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL);

        root.setBackgroundColor(
                Color.rgb(245, 248, 246));

        TextView header =
                new TextView(this);

        header.setText("🛺  SAKAY NA");
        header.setTextSize(25);
        header.setTypeface(
                null,
                Typeface.BOLD);
        header.setTextColor(Color.WHITE);
        header.setGravity(Gravity.CENTER);
        header.setPadding(
                10,
                20,
                10,
                20);

        header.setBackgroundColor(
                Color.rgb(0, 125, 75));

        root.addView(
                header,
                new LinearLayout.LayoutParams(
                        -1,
                        -2));

        TextView subtitle =
                new TextView(this);

        if ("LIVE_RIDE".equalsIgnoreCase(mode)) {

            subtitle.setText(
                    "🚦 LIVE RIDE • Your trip is in progress");

        } else {

            subtitle.setText(
                    "📍 Search a place or choose on the map");
        }

        subtitle.setTextSize(16);
        subtitle.setTypeface(
                null,
                Typeface.BOLD);
        subtitle.setTextColor(
                Color.rgb(30, 80, 55));
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(
                12,
                12,
                12,
                8);

        root.addView(subtitle);

        if ("SELECT_DESTINATION"
                .equalsIgnoreCase(mode)) {

            LinearLayout searchRow =
                    new LinearLayout(this);

            searchRow.setOrientation(
                    LinearLayout.HORIZONTAL);

            searchRow.setPadding(
                    10,
                    4,
                    10,
                    4);

            searchInput =
                    new EditText(this);

            searchInput.setHint(
                    "Search Jollibee, SM, barangay, street...");
            searchInput.setTextSize(15);
            searchInput.setSingleLine(true);

            LinearLayout.LayoutParams inputParams =
                    new LinearLayout.LayoutParams(
                            0,
                            -2,
                            1f);

            inputParams.setMargins(
                    0,
                    0,
                    6,
                    0);

            searchRow.addView(
                    searchInput,
                    inputParams);

            searchButton =
                    makeButton("🔎 SEARCH");

            LinearLayout.LayoutParams searchParams =
                    new LinearLayout.LayoutParams(
                            130,
                            -2);

            searchRow.addView(
                    searchButton,
                    searchParams);

            searchButton.setOnClickListener(
                    v -> searchPlace());

            searchInput.setOnEditorActionListener(
                    (v, actionId, event) -> {
                        searchPlace();
                        return true;
                    });

            root.addView(searchRow);

            searchResultsContainer =
                    new LinearLayout(this);

            searchResultsContainer.setOrientation(
                    LinearLayout.VERTICAL);

            searchResultsContainer.setPadding(
                    10,
                    2,
                    10,
                    4);

            root.addView(
                    searchResultsContainer,
                    new LinearLayout.LayoutParams(
                            -1,
                            -2));
        }

        destinationText =
                new TextView(this);

        if (destinationChosen) {

            destinationText.setText(
                    "🎯 Destination:\n" +
                    destinationAddress);

        } else {

            destinationText.setText(
                    "🎯 Destination not selected\n" +
                    "Search for a place or tap the map.");
        }

        destinationText.setTextSize(15);
        destinationText.setTextColor(Color.DKGRAY);
        destinationText.setGravity(Gravity.CENTER);
        destinationText.setPadding(
                14,
                8,
                14,
                8);

        root.addView(destinationText);

        statusText =
                new TextView(this);

        statusText.setText(
                "🗺️ Loading OpenStreetMap...");

        statusText.setTextSize(14);
        statusText.setTextColor(Color.GRAY);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(
                10,
                4,
                10,
                8);

        root.addView(statusText);

        webView =
                new WebView(this);

        LinearLayout.LayoutParams mapParams =
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1);

        mapParams.setMargins(
                10,
                4,
                10,
                6);

        root.addView(
                webView,
                mapParams);

        LinearLayout controls =
                new LinearLayout(this);

        controls.setOrientation(
                LinearLayout.VERTICAL);

        controls.setGravity(
                Gravity.CENTER_HORIZONTAL);

        controls.setPadding(
                12,
                4,
                12,
                10);

        gpsButton =
                makeButton(
                        "📍 CURRENT GPS");

        gpsButton.setOnClickListener(
                v -> centerOnGPS());

        controls.addView(
                gpsButton,
                buttonParams());

        if ("SELECT_DESTINATION"
                .equalsIgnoreCase(mode)) {

            destinationButton =
                    makeButton(
                            "🎯 SET DESTINATION");

            destinationButton.setEnabled(
                    destinationChosen);

            destinationButton.setOnClickListener(
                    v -> returnDestination());

            controls.addView(
                    destinationButton,
                    buttonParams());
        }

        liveMapButton =
                makeButton(
                        "🚦 LIVE MAP");

        liveMapButton.setOnClickListener(
                v -> {

                    if ("LIVE_RIDE"
                            .equalsIgnoreCase(mode)) {

                        Toast.makeText(
                                this,
                                "You are already on the live map.",
                                Toast.LENGTH_SHORT
                        ).show();

                    } else {

                        Toast.makeText(
                                this,
                                "Live map becomes available after a ride is active.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });

        controls.addView(
                liveMapButton,
                buttonParams());

        backButton =
                makeButton(
                        "← BACK / HOME");

        backButton.setOnClickListener(
                v -> finish());

        controls.addView(
                backButton,
                buttonParams());

        root.addView(controls);

        setContentView(root);
    }

    private Button makeButton(String text) {

        Button button =
                new Button(this);

        button.setText(text);
        button.setTextSize(14);
        button.setTypeface(
                null,
                Typeface.BOLD);
        button.setTextColor(Color.WHITE);
        button.setGravity(Gravity.CENTER);
        button.setAllCaps(false);

        button.setBackgroundColor(
                Color.rgb(0, 125, 75));

        button.setPadding(
                8,
                10,
                8,
                10);

        return button;
    }

    private LinearLayout.LayoutParams buttonParams() {

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        -1,
                        -2);

        params.setMargins(
                0,
                3,
                0,
                3);

        return params;
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
                    "Enter a place name.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (searchButton != null) {
            searchButton.setEnabled(false);
            searchButton.setText("SEARCHING...");
        }

        if (searchResultsContainer != null) {
            searchResultsContainer.removeAllViews();
        }

        statusText.setText(
                "🔎 Searching for " + query + "...");

        new Thread(() -> {

            JSONArray results = null;
            String errorMessage = "";

            HttpURLConnection connection = null;

            try {

                String encoded =
                        URLEncoder.encode(
                                query,
                                "UTF-8");

                String urlString =
                        "https://nominatim.openstreetmap.org/search" +
                        "?format=jsonv2" +
                        "&q=" + encoded +
                        "&limit=5" +
                        "&addressdetails=1";

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
                        "SakayNa/1.0 Android");

                connection.setRequestProperty(
                        "Accept",
                        "application/json");

                int responseCode =
                        connection.getResponseCode();

                if (responseCode != 200) {

                    errorMessage =
                            "Search service unavailable.";
                } else {

                    BufferedReader reader =
                            new BufferedReader(
                                    new InputStreamReader(
                                            connection.getInputStream()));

                    StringBuilder json =
                            new StringBuilder();

                    String line;

                    while ((line =
                            reader.readLine()) != null) {

                        json.append(line);
                    }

                    reader.close();

                    results =
                            new JSONArray(
                                    json.toString());
                }

            } catch (Exception e) {

                errorMessage =
                        "Could not search this place.";

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

            JSONArray finalResults = results;
            String finalError = errorMessage;

            handler.post(() -> {

                if (searchButton != null) {

                    searchButton.setEnabled(true);
                    searchButton.setText("🔎 SEARCH");
                }

                if (!finalError.isEmpty()) {

                    statusText.setText(
                            "🔴 " + finalError);

                    Toast.makeText(
                            this,
                            finalError,
                            Toast.LENGTH_SHORT
                    ).show();

                    return;
                }

                if (finalResults == null ||
                        finalResults.length() == 0) {

                    statusText.setText(
                            "❌ No place found. Try another name.");

                    Toast.makeText(
                            this,
                            "No place found.",
                            Toast.LENGTH_SHORT
                    ).show();

                    return;
                }

                showSearchResults(finalResults);
            });

        }).start();
    }

    private void showSearchResults(
            JSONArray results) {

        if (searchResultsContainer == null) {
            return;
        }

        searchResultsContainer.removeAllViews();

        statusText.setText(
                "📍 Select your destination:");

        for (int i = 0;
                i < results.length();
                i++) {

            try {

                JSONObject item =
                        results.getJSONObject(i);

                final double lat =
                        Double.parseDouble(
                                item.getString("lat"));

                final double lon =
                        Double.parseDouble(
                                item.getString("lon"));

                final String name =
                        item.optString(
                                "display_name",
                                "Selected place");

                Button resultButton =
                        new Button(this);

                resultButton.setText(
                        "📍 " + name);

                resultButton.setTextSize(13);
                resultButton.setTextColor(
                        Color.rgb(20, 70, 45));

                resultButton.setAllCaps(false);
                resultButton.setGravity(
                        Gravity.START | Gravity.CENTER_VERTICAL);

                resultButton.setPadding(
                        12,
                        8,
                        12,
                        8);

                resultButton.setBackgroundColor(
                        Color.WHITE);

                resultButton.setOnClickListener(
                        v -> selectSearchResult(
                                lat,
                                lon,
                                name));

                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(
                                -1,
                                -2);

                params.setMargins(
                        0,
                        2,
                        0,
                        2);

                searchResultsContainer.addView(
                        resultButton,
                        params);

            } catch (Exception ignored) {
            }
        }
    }

    private void selectSearchResult(
            double lat,
            double lng,
            String name) {

        destinationLatitude = lat;
        destinationLongitude = lng;
        destinationAddress = name;
        destinationChosen = true;

        destinationText.setText(
                "🎯 Destination:\n" +
                destinationAddress);

        statusText.setText(
                "✅ Destination selected");

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
                lat + "," +
                lng +
                ",'" +
                escapeJS(name) +
                "');");

        Toast.makeText(
                this,
                "Destination selected.",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void setupMap() {

        WebSettings settings =
                webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(true);

        settings.setLoadWithOverviewMode(false);
        settings.setUseWideViewPort(false);

        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.setClickable(true);

        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);

        webView.setWebViewClient(
                new WebViewClient());

        webView.setWebChromeClient(
                new WebChromeClient());

        webView.addJavascriptInterface(
                new MapBridge(),
                "Android");

        webView.loadDataWithBaseURL(
                "https://sakyna-map.local/",
                mapHtml(),
                "text/html",
                "UTF-8",
                null);
    }

    private String mapHtml() {

        String lat =
                pickupLatitude != 0
                        ? String.format(
                        Locale.US,
                        "%.7f",
                        pickupLatitude)
                        : "14.000000";

        String lng =
                pickupLongitude != 0
                        ? String.format(
                        Locale.US,
                        "%.7f",
                        pickupLongitude)
                        : "121.000000";

        return "<!DOCTYPE html>" +
                "<html><head>" +

                "<meta name='viewport' " +
                "content='width=device-width," +
                "initial-scale=1.0," +
                "maximum-scale=1.0," +
                "user-scalable=yes'>" +

                "<link rel='stylesheet' " +
                "href='https://unpkg.com/" +
                "leaflet@1.9.4/dist/leaflet.css'>" +

                "<script src='https://unpkg.com/" +
                "leaflet@1.9.4/dist/leaflet.js'>" +
                "</script>" +

                "<style>" +
                "html,body,#map{" +
                "height:100%;" +
                "width:100%;" +
                "margin:0;" +
                "padding:0;" +
                "overflow:hidden;" +
                "}" +
                "</style>" +

                "</head><body>" +

                "<div id='map'></div>" +

                "<script>" +

                "var map=L.map('map'," +
                "{zoomControl:true," +
                "dragging:true," +
                "touchZoom:true," +
                "scrollWheelZoom:true," +
                "doubleClickZoom:true});" +

                "var userMarker=null;" +
                "var destinationMarker=null;" +
                "var driverMarker=null;" +

                "map.setView([" +
                lat + "," +
                lng + "],15);" +

                "L.tileLayer(" +
                "'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'," +
                "{maxZoom:19," +
                "attribution:'© OpenStreetMap contributors'}" +
                ").addTo(map);" +

                "function updateUser(lat,lng,center){" +
                "if(userMarker===null){" +
                "userMarker=L.marker([lat,lng])" +
                ".addTo(map)" +
                ".bindPopup('📍 You are here');" +
                "}else{" +
                "userMarker.setLatLng([lat,lng]);" +
                "}" +
                "if(center)map.setView([lat,lng],17);" +
                "}" +

                "function setDestination(lat,lng,address){" +
                "if(destinationMarker!==null)" +
                "map.removeLayer(destinationMarker);" +
                "destinationMarker=L.marker([lat,lng])" +
                ".addTo(map)" +
                ".bindPopup('🎯 '+address)" +
                ".openPopup();" +
                "map.setView([lat,lng],17);" +
                "}" +

                "function setDriver(lat,lng){" +
                "if(driverMarker===null){" +
                "driverMarker=L.marker([lat,lng])" +
                ".addTo(map)" +
                ".bindPopup('🛺 Driver');" +
                "}else{" +
                "driverMarker.setLatLng([lat,lng]);" +
                "}" +
                "}" +

                "map.on('click',function(e){" +
                "if(window.Android)" +
                "Android.mapClicked(" +
                "e.latlng.lat,e.latlng.lng);" +
                "});" +

                "map.whenReady(function(){" +
                "if(window.Android)" +
                "Android.mapReady();" +
                "});" +

                "</script></body></html>";
    }

    private void requestLocation() {

        if (
                checkSelfPermission(
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED
                        &&
                checkSelfPermission(
                        Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED
        ) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST);

            return;
        }

        startLocation();
    }

    private void startLocation() {

        locationManager =
                (LocationManager)
                        getSystemService(
                                LOCATION_SERVICE);

        if (locationManager == null) {
            return;
        }

        locationListener =
                new LocationListener() {

                    @Override
                    public void onLocationChanged(
                            @NonNull Location location) {

                        pickupLatitude =
                                location.getLatitude();

                        pickupLongitude =
                                location.getLongitude();

                        if (mapReady) {

                            sendJS(
                                    "updateUser(" +
                                    pickupLatitude +
                                    "," +
                                    pickupLongitude +
                                    ",false);");
                        }
                    }
                };

        try {

            if (
                    checkSelfPermission(
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED
                            ||
                    checkSelfPermission(
                            Manifest.permission.ACCESS_COARSE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED
            ) {

                if (locationManager.isProviderEnabled(
                        LocationManager.GPS_PROVIDER)) {

                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            3000,
                            5,
                            locationListener);
                }

                if (locationManager.isProviderEnabled(
                        LocationManager.NETWORK_PROVIDER)) {

                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            3000,
                            5,
                            locationListener);
                }
            }

        } catch (SecurityException ignored) {
        }

        if ("LIVE_RIDE".equalsIgnoreCase(mode)) {
            startLiveRide();
        }
    }

    private void centerOnGPS() {

        if (pickupLatitude == 0 ||
                pickupLongitude == 0) {

            Toast.makeText(
                    this,
                    "Waiting for GPS location...",
                    Toast.LENGTH_SHORT
            ).show();

            requestLocation();
            return;
        }

        sendJS(
                "updateUser(" +
                pickupLatitude +
                "," +
                pickupLongitude +
                ",true);");
    }

    private void sendJS(String javascript) {

        if (webView == null) {
            return;
        }

        webView.post(
                () -> webView.evaluateJavascript(
                        javascript,
                        null));
    }

    private void mapReady() {

        mapReady = true;

        if (pickupLatitude != 0 &&
                pickupLongitude != 0) {

            sendJS(
                    "updateUser(" +
                    pickupLatitude +
                    "," +
                    pickupLongitude +
                    ",true);");
        }

        if (destinationChosen) {

            sendJS(
                    "setDestination(" +
                    destinationLatitude +
                    "," +
                    destinationLongitude +
                    ",'" +
                    escapeJS(destinationAddress) +
                    "');");
        }

        statusText.setText(
                "🗺️ Map ready • Search a place or tap the map");
    }

    private String escapeJS(String text) {

        if (text == null) {
            return "";
        }

        return text
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", " ")
                .replace("\r", " ");
    }

    private void mapClicked(
            double lat,
            double lng) {

        if (!"SELECT_DESTINATION"
                .equalsIgnoreCase(mode)) {

            return;
        }

        destinationLatitude = lat;
        destinationLongitude = lng;
        destinationChosen = true;

        statusText.setText(
                "🎯 Destination selected • Finding address...");

        reverseGeocode(lat, lng);
    }

    private void reverseGeocode(
            double lat,
            double lng) {

        new Thread(() -> {

            String address = "";

            try {

                Geocoder geocoder =
                        new Geocoder(
                                this,
                                Locale.getDefault());

                List<Address> results =
                        geocoder.getFromLocation(
                                lat,
                                lng,
                                1);

                if (results != null &&
                        !results.isEmpty()) {

                    Address a =
                            results.get(0);

                    if (a.getAddressLine(0) != null) {

                        address =
                                a.getAddressLine(0);
                    }
                }

            } catch (Exception ignored) {
            }

            final String finalAddress =
                    address.isEmpty()
                            ? String.format(
                            Locale.US,
                            "%.6f, %.6f",
                            lat,
                            lng)
                            : address;

            handler.post(() -> {

                destinationAddress =
                        finalAddress;

                destinationText.setText(
                        "🎯 Destination:\n" +
                        destinationAddress);

                statusText.setText(
                        "✅ Destination selected");

                if (destinationButton != null) {
                    destinationButton.setEnabled(true);
                }

                sendJS(
                        "setDestination(" +
                        lat +
                        "," +
                        lng +
                        ",'" +
                        escapeJS(finalAddress) +
                        "');");
            });

        }).start();
    }

    private void returnDestination() {

        if (!destinationChosen) {

            Toast.makeText(
                    this,
                    "Please select a destination first.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Intent result =
                new Intent();

        result.putExtra(
                "destination_latitude",
                destinationLatitude);

        result.putExtra(
                "destination_longitude",
                destinationLongitude);

        result.putExtra(
                "destination_address",
                destinationAddress);

        result.putExtra(
                "destinationName",
                destinationAddress);

        setResult(
                RESULT_OK,
                result);

        finish();
    }

    private void startLiveRide() {

        if (rideId == null ||
                rideId.trim().isEmpty()) {

            return;
        }

        rideListener =
                db.collection("rides")
                        .document(rideId)
                        .addSnapshotListener(
                                (snapshot, error) -> {

                                    if (error != null ||
                                            snapshot == null ||
                                            !snapshot.exists()) {

                                        return;
                                    }

                                    Double pLat =
                                            snapshot.getDouble(
                                                    "pickupLatitude");

                                    Double pLng =
                                            snapshot.getDouble(
                                                    "pickupLongitude");

                                    Double dLat =
                                            snapshot.getDouble(
                                                    "destinationLatitude");

                                    Double dLng =
                                            snapshot.getDouble(
                                                    "destinationLongitude");

                                    if (pLat != null &&
                                            pLng != null) {

                                        pickupLatitude =
                                                pLat;

                                        pickupLongitude =
                                                pLng;

                                        sendJS(
                                                "updateUser(" +
                                                pLat +
                                                "," +
                                                pLng +
                                                ",false);");
                                    }

                                    if (dLat != null &&
                                            dLng != null) {

                                        destinationLatitude =
                                                dLat;

                                        destinationLongitude =
                                                dLng;

                                        destinationChosen =
                                                true;

                                        sendJS(
                                                "setDestination(" +
                                                dLat +
                                                "," +
                                                dLng +
                                                ",'Destination');");
                                    }

                                    String status =
                                            snapshot.getString(
                                                    "status");

                                    if (status != null) {

                                        statusText.setText(
                                                "🚦 Ride status: " +
                                                status);
                                    }

                                    String driverId =
                                            snapshot.getString(
                                                    "driverId");

                                    if (driverId != null &&
                                            !driverId.trim().isEmpty()) {

                                        listenDriver(driverId);
                                    }
                                });
    }

    private void listenDriver(
            String driverId) {

        if (driverListener != null) {
            driverListener.remove();
        }

        driverListener =
                db.collection("driverLocations")
                        .document(driverId)
                        .addSnapshotListener(
                                (snapshot, error) -> {

                                    if (error != null ||
                                            snapshot == null ||
                                            !snapshot.exists()) {

                                        return;
                                    }

                                    Double lat =
                                            snapshot.getDouble(
                                                    "latitude");

                                    Double lng =
                                            snapshot.getDouble(
                                                    "longitude");

                                    if (lat == null ||
                                            lng == null) {

                                        return;
                                    }

                                    driverLatitude = lat;
                                    driverLongitude = lng;

                                    sendJS(
                                            "setDriver(" +
                                            lat +
                                            "," +
                                            lng +
                                            ");");
                                });
    }

    public class MapBridge {

        @JavascriptInterface
        public void mapReady() {

            runOnUiThread(
                    () -> MapActivity.this.mapReady());
        }

        @JavascriptInterface
        public void mapClicked(
                double lat,
                double lng) {

            runOnUiThread(
                    () -> MapActivity.this.mapClicked(
                            lat,
                            lng));
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] results) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                results);

        if (requestCode ==
                LOCATION_PERMISSION_REQUEST &&
                results.length > 0 &&
                (
                        results[0] ==
                                PackageManager.PERMISSION_GRANTED
                        ||
                        (
                                results.length > 1 &&
                                results[1] ==
                                        PackageManager.PERMISSION_GRANTED
                        )
                )) {

            startLocation();
        }
    }

    @Override
    protected void onDestroy() {

        if (rideListener != null) {
            rideListener.remove();
            rideListener = null;
        }

        if (driverListener != null) {
            driverListener.remove();
            driverListener = null;
        }

        if (locationManager != null &&
                locationListener != null) {

            try {

                locationManager.removeUpdates(
                        locationListener);

            } catch (SecurityException ignored) {
            }
        }

        if (webView != null) {

            webView.stopLoading();

            webView.removeJavascriptInterface(
                    "Android");

            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }
}
