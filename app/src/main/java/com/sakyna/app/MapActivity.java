package com.sakyna.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
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
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
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

    private Button myLocationButton;
    private Button destinationButton;

    private LocationManager locationManager;
    private LocationListener locationListener;

    private FirebaseFirestore db;

    private ListenerRegistration rideListener;
    private ListenerRegistration driverListener;

    private String mode = "SELECT_DESTINATION";
    private String rideId = "";

    /*
     * Safe fallback only.
     * Real GPS replaces this as soon as available.
     */
    private double currentLatitude = 14.4297;
    private double currentLongitude = 120.9367;

    private double destinationLatitude = 0;
    private double destinationLongitude = 0;
    private String destinationAddress = "";

    private boolean destinationChosen = false;
    private boolean mapReady = false;

    /*
     * Prevent repeated POI downloads while GPS is moving.
     */
    private boolean loadingNearbyPlaces = false;
    private double lastPoiLatitude = 0;
    private double lastPoiLongitude = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();

        String receivedMode =
                getIntent().getStringExtra("mode");

        if (receivedMode != null
                && !receivedMode.trim().isEmpty()) {

            mode = receivedMode.trim();
        }

        rideId =
                getIntent().getStringExtra("ride_id");

        if (rideId == null
                || rideId.trim().isEmpty()) {

            rideId =
                    getIntent().getStringExtra("rideId");
        }

        if (rideId == null) {
            rideId = "";
        }

        rideId = rideId.trim();

        buildScreen();
        startLocationTracking();

        if ("LIVE_RIDE".equals(mode)) {
            setupLiveRide();
        }
    }

    private void buildScreen() {

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setBackgroundColor(
                Color.rgb(247, 252, 250)
        );

        TextView title =
                new TextView(this);

        title.setText(
                "LIVE_RIDE".equals(mode)
                        ? "🛺 SAKAY NA — LIVE RIDE"
                        : "📍 CHOOSE DESTINATION"
        );

        title.setTextSize(23);
        title.setTypeface(
                null,
                Typeface.BOLD
        );

        title.setTextColor(
                Color.rgb(0, 120, 70)
        );

        title.setGravity(Gravity.CENTER);

        title.setPadding(
                10,
                18,
                10,
                12
        );

        root.addView(title);

        if ("SELECT_DESTINATION".equals(mode)) {
            buildSearchArea(root);
        }

        statusText =
                new TextView(this);

        statusText.setText(
                "📍 Connecting to GPS..."
        );

        statusText.setTextSize(14);
        statusText.setTextColor(Color.DKGRAY);
        statusText.setGravity(Gravity.CENTER);

        statusText.setPadding(
                10,
                5,
                10,
                5
        );

        root.addView(statusText);

        destinationText =
                new TextView(this);

        destinationText.setText(
                "Move the map or search for a destination."
        );

        destinationText.setTextSize(15);
        destinationText.setTextColor(Color.DKGRAY);

        destinationText.setPadding(
                15,
                6,
                15,
                6
        );

        root.addView(destinationText);

        webView =
                new WebView(this);

        WebSettings settings =
                webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        webView.setBackgroundColor(
                Color.rgb(238, 247, 243)
        );

        webView.setWebViewClient(
                new WebViewClient() {

                    @Override
                    public void onPageFinished(
                            WebView view,
                            String url
                    ) {

                        super.onPageFinished(
                                view,
                                url
                        );

                        mapReady = true;

                        sendCurrentLocationToMap();

                        statusText.setText(
                                "🟢 Live GPS map ready"
                        );

                        /*
                         * Load nearby free OSM places once
                         * the map is ready.
                         */
                        loadNearbyPlaces(
                                currentLatitude,
                                currentLongitude
                        );
                    }
                }
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

        myLocationButton =
                new Button(this);

        myLocationButton.setText(
                "📍 MY LOCATION"
        );

        myLocationButton.setTextSize(16);
        myLocationButton.setTextColor(Color.WHITE);

        myLocationButton.setBackgroundColor(
                Color.rgb(0, 120, 215)
        );

        myLocationButton.setOnClickListener(
                v -> recenterOnMyLocation()
        );

        root.addView(myLocationButton);

        if ("SELECT_DESTINATION".equals(mode)) {

            destinationButton =
                    new Button(this);

            destinationButton.setText(
                    "✅ USE THIS DESTINATION"
            );

            destinationButton.setTextSize(16);
            destinationButton.setTextColor(Color.WHITE);

            destinationButton.setBackgroundColor(
                    Color.rgb(0, 135, 80)
            );

            destinationButton.setEnabled(false);

            destinationButton.setOnClickListener(
                    v -> returnDestination()
            );

            root.addView(destinationButton);
        }

        Button backButton =
                new Button(this);

        backButton.setText("⬅ BACK");

        backButton.setOnClickListener(
                v -> finish()
        );

        root.addView(backButton);

        setContentView(root);

        loadMap();
    }

    private void buildSearchArea(
            LinearLayout root
    ) {

        LinearLayout searchRow =
                new LinearLayout(this);

        searchRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        searchRow.setPadding(
                8,
                4,
                8,
                4
        );

        searchInput =
                new EditText(this);

        searchInput.setHint(
                "Search Jollibee, SM, street..."
        );

        searchInput.setTextSize(16);
        searchInput.setSingleLine(true);

        searchRow.addView(
                searchInput,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        Button searchButton =
                new Button(this);

        searchButton.setText("🔎 SEARCH");
        searchButton.setTextColor(Color.WHITE);

        searchButton.setBackgroundColor(
                Color.rgb(0, 135, 80)
        );

        searchButton.setOnClickListener(
                v -> searchPlace()
        );

        searchRow.addView(searchButton);

        root.addView(searchRow);

        ScrollView scroll =
                new ScrollView(this);

        searchResultsContainer =
                new LinearLayout(this);

        searchResultsContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        scroll.addView(
                searchResultsContainer
        );

        root.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        145
                )
        );
    }

    private void loadMap() {

        String html =
                "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +

                "<meta name='viewport' " +
                "content='width=device-width," +
                "initial-scale=1.0," +
                "maximum-scale=1.0," +
                "user-scalable=yes'>" +

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
                "background:#eef7f3;" +
                "}" +

                ".leaflet-control-zoom a{" +
                "font-size:22px;" +
                "}" +

                ".poi-label{" +
                "background:white;" +
                "border:1px solid #777;" +
                "border-radius:5px;" +
                "padding:2px 5px;" +
                "font-size:11px;" +
                "font-weight:bold;" +
                "white-space:nowrap;" +
                "box-shadow:0 1px 4px rgba(0,0,0,.30);" +
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
                "],15);" +

                "L.tileLayer(" +
                "'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'," +
                "{" +
                "maxZoom:19," +
                "attribution:'© OpenStreetMap contributors'" +
                "}" +
                ").addTo(map);" +

                /*
                 * Scale makes the map easier to understand.
                 */
                "L.control.scale({" +
                "imperial:false" +
                "}).addTo(map);" +

                "var userMarker=null;" +
                "var accuracyCircle=null;" +
                "var destinationMarker=null;" +
                "var driverMarker=null;" +
                "var poiLayer=L.layerGroup().addTo(map);" +

                "var hasInitialCenter=false;" +
                "var hasInitialDriverCenter=false;" +
                "var followUser=false;" +

                "function createUserMarker(lat,lng){" +

                "var icon=L.divIcon({" +
                "className:'sakayna-location'," +
                "html:" +
                "'<div style=\"" +
                "width:18px;" +
                "height:18px;" +
                "background:#1976d2;" +
                "border:4px solid white;" +
                "border-radius:50%;" +
                "box-shadow:0 1px 6px rgba(0,0,0,.45);" +
                "\"></div>'," +
                "iconSize:[26,26]," +
                "iconAnchor:[13,13]" +
                "});" +

                "userMarker=" +
                "L.marker([lat,lng],{" +
                "icon:icon," +
                "zIndexOffset:1000" +
                "}).addTo(map)" +
                ".bindPopup('📍 You are here');" +

                "}" +

                "function setUser(lat,lng,accuracy){" +

                "if(userMarker==null){" +
                "createUserMarker(lat,lng);" +
                "}else{" +
                "userMarker.setLatLng([lat,lng]);" +
                "}" +

                "if(accuracy && accuracy>0){" +

                "if(accuracyCircle==null){" +

                "accuracyCircle=" +
                "L.circle([lat,lng],{" +
                "radius:accuracy," +
                "color:'#1976d2'," +
                "fillColor:'#1976d2'," +
                "fillOpacity:.10," +
                "weight:1" +
                "}).addTo(map);" +

                "}else{" +

                "accuracyCircle.setLatLng([lat,lng]);" +
                "accuracyCircle.setRadius(accuracy);" +

                "}" +

                "}" +

                "if(!hasInitialCenter){" +
                "map.setView([lat,lng],17);" +
                "hasInitialCenter=true;" +
                "}" +

                "if(followUser){" +

                "map.setView([lat,lng],17,{" +
                "animate:true" +
                "});" +

                "}" +

                "}" +

                "function setFollow(value){" +

                "followUser=value;" +

                "if(followUser && userMarker!=null){" +

                "var p=userMarker.getLatLng();" +

                "map.setView([p.lat,p.lng],17,{" +
                "animate:true" +
                "});" +

                "}" +

                "}" +

                "function setDestination(lat,lng,name){" +

                "if(destinationMarker!=null){" +
                "map.removeLayer(destinationMarker);" +
                "}" +

                "destinationMarker=" +
                "L.marker([lat,lng])" +
                ".addTo(map)" +
                ".bindPopup(" +
                "name" +
                ").openPopup();" +

                "map.setView([lat,lng],17);" +

                "hasInitialCenter=true;" +

                "followUser=false;" +

                "}" +

                "function setDriver(lat,lng){" +

                "if(driverMarker==null){" +

                "var driverIcon=" +
                "L.divIcon({" +
                "className:'sakayna-driver'," +
                "html:'<div style=\"" +
                "font-size:30px;" +
                "line-height:30px;" +
                "\">🛺</div>'," +
                "iconSize:[34,34]," +
                "iconAnchor:[17,17]" +
                "});" +

                "driverMarker=" +
                "L.marker([lat,lng],{" +
                "icon:driverIcon," +
                "zIndexOffset:900" +
                "}).addTo(map)" +
                ".bindPopup('🛺 Driver');" +

                "}else{" +

                "driverMarker.setLatLng([lat,lng]);" +

                "}" +

                "if(!hasInitialDriverCenter){" +
                "map.setView([lat,lng],17);" +
                "hasInitialDriverCenter=true;" +
                "}" +

                "}" +

                /*
                 * Add nearby OSM places.
                 */
                "function clearPOIs(){" +
                "poiLayer.clearLayers();" +
                "}" +

                "function addPOI(lat,lng,name,type){" +

                "if(!name || name.length===0){" +
                "return;" +
                "}" +

                "var emoji='📍';" +

                "if(type==='restaurant') emoji='🍽️';" +
                "else if(type==='cafe') emoji='☕';" +
                "else if(type==='school') emoji='🏫';" +
                "else if(type==='hospital') emoji='🏥';" +
                "else if(type==='pharmacy') emoji='💊';" +
                "else if(type==='fuel') emoji='⛽';" +
                "else if(type==='bank') emoji='🏦';" +
                "else if(type==='place_of_worship') emoji='⛪';" +
                "else if(type==='mall') emoji='🏬';" +
                "else if(type==='supermarket') emoji='🛒';" +
                "else if(type==='convenience') emoji='🏪';" +

                "var icon=L.divIcon({" +
                "className:'sakayna-poi'," +
                "html:'<div style=\"" +
                "font-size:18px;" +
                "background:white;" +
                "border-radius:50%;" +
                "width:28px;" +
                "height:28px;" +
                "line-height:28px;" +
                "text-align:center;" +
                "border:1px solid #777;" +
                "box-shadow:0 1px 4px rgba(0,0,0,.35);" +
                "\">'+emoji+'</div>'," +
                "iconSize:[28,28]," +
                "iconAnchor:[14,14]" +
                "});" +

                "L.marker([lat,lng],{" +
                "icon:icon," +
                "zIndexOffset:300" +
                "}).addTo(poiLayer)" +
                ".bindPopup(" +
                "escapeHtml(name)" +
                "+'<br><small>OpenStreetMap place</small>'" +
                ");" +

                "}" +

                "function escapeHtml(value){" +

                "return String(value)" +
                ".replace(/&/g,'&amp;')" +
                ".replace(/</g,'&lt;')" +
                ".replace(/>/g,'&gt;')" +
                ".replace(/\"/g,'&quot;')" +
                ".replace(/'/g,'&#039;');" +

                "}" +

                "map.on('dragstart',function(){" +
                "followUser=false;" +
                "});" +

                "map.on('click',function(e){" +

                "AndroidMap.tap(" +
                "e.latlng.lat," +
                "e.latlng.lng" +
                ");" +

                "});" +

                "</script>" +

                "</body>" +
                "</html>";

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
        public void tap(
                double lat,
                double lng
        ) {

            runOnUiThread(() -> {

                if ("SELECT_DESTINATION".equals(mode)) {

                    reverseGeocode(
                            lat,
                            lng
                    );
                }
            });
        }
    }

    private void startLocationTracking() {

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

                        sendLocationToMap(
                                location
                        );

                        statusText.setText(
                                String.format(
                                        Locale.US,
                                        "🟢 LIVE GPS\nAccuracy: %.0f m",
                                        location.getAccuracy()
                                )
                        );

                        /*
                         * Load nearby places when the real
                         * GPS position becomes available.
                         */
                        if (mapReady) {

                            loadNearbyPlacesIfNeeded(
                                    currentLatitude,
                                    currentLongitude
                            );
                        }
                    }
                };

        try {

            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000,
                    1,
                    locationListener,
                    Looper.getMainLooper()
            );

            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    2000,
                    1,
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

                currentLatitude =
                        last.getLatitude();

                currentLongitude =
                        last.getLongitude();

                sendLocationToMap(last);

                if (mapReady) {

                    loadNearbyPlaces(
                            currentLatitude,
                            currentLongitude
                    );
                }
            }

        } catch (SecurityException e) {

            statusText.setText(
                    "❌ Location permission required."
            );

        } catch (Exception e) {

            statusText.setText(
                    "❌ Unable to start GPS."
            );
        }
    }

    private void sendLocationToMap(
            Location location
    ) {

        if (!mapReady
                || webView == null) {
            return;
        }

        double lat =
                location.getLatitude();

        double lng =
                location.getLongitude();

        float accuracy =
                location.hasAccuracy()
                        ? location.getAccuracy()
                        : 0;

        String javascript =
                "setUser(" +
                lat +
                "," +
                lng +
                "," +
                accuracy +
                ");";

        runOnUiThread(() ->
                webView.evaluateJavascript(
                        javascript,
                        null
                )
        );
    }

    private void sendCurrentLocationToMap() {

        if (!mapReady
                || webView == null) {
            return;
        }

        String javascript =
                "setUser(" +
                currentLatitude +
                "," +
                currentLongitude +
                ",0);";

        runOnUiThread(() ->
                webView.evaluateJavascript(
                        javascript,
                        null
                )
        );
    }

    private void recenterOnMyLocation() {

        if (!mapReady
                || webView == null) {
            return;
        }

        webView.evaluateJavascript(
                "setFollow(true);",
                null
        );

        Toast.makeText(
                this,
                "📍 Following your live location",
                Toast.LENGTH_SHORT
        ).show();
    }

    /*
     * Load nearby OSM places only after moving
     * a reasonable distance from the previous query.
     */
    private void loadNearbyPlacesIfNeeded(
            double lat,
            double lng
    ) {

        if (lastPoiLatitude == 0
                && lastPoiLongitude == 0) {

            loadNearbyPlaces(lat, lng);
            return;
        }

        float[] distance =
                new float[1];

        Location.distanceBetween(
                lastPoiLatitude,
                lastPoiLongitude,
                lat,
                lng,
                distance
        );

        /*
         * Refresh only after approximately 500 meters.
         */
        if (distance[0] >= 500) {

            loadNearbyPlaces(
                    lat,
                    lng
            );
        }
    }

    /*
     * Free OpenStreetMap POI lookup using Overpass.
     *
     * This does not require Google Maps billing.
     */
    private void loadNearbyPlaces(
            double lat,
            double lng
    ) {

        if (!mapReady
                || webView == null
                || loadingNearbyPlaces) {

            return;
        }

        loadingNearbyPlaces = true;

        lastPoiLatitude = lat;
        lastPoiLongitude = lng;

        new Thread(() -> {

            HttpURLConnection connection = null;

            try {

                String query =
                        "[out:json][timeout:15];" +
                        "(" +

                        "nwr(around:1200," +
                        lat + "," +
                        lng +
                        ")[name][amenity~" +
                        "\"restaurant|cafe|school|hospital|" +
                        "pharmacy|fuel|bank|place_of_worship|" +
                        "fast_food|clinic\"" +
                        "];" +

                        "nwr(around:1200," +
                        lat + "," +
                        lng +
                        ")[name][shop~" +
                        "\"supermarket|convenience|mall|bakery|" +
                        "department_store\"" +
                        "];" +

                        "nwr(around:1200," +
                        lat + "," +
                        lng +
                        ")[name][tourism~" +
                        "\"hotel|attraction|museum\"" +
                        "];" +

                        ");" +

                        "out center tags;";

                String encoded =
                        URLEncoder.encode(
                                query,
                                "UTF-8"
                        );

                String urlString =
                        "https://overpass-api.de/api/interpreter" +
                        "?data=" +
                        encoded;

                URL url =
                        new URL(urlString);

                connection =
                        (HttpURLConnection)
                                url.openConnection();

                connection.setRequestMethod("GET");

                connection.setConnectTimeout(
                        15000
                );

                connection.setReadTimeout(
                        20000
                );

                connection.setRequestProperty(
                        "User-Agent",
                        "SakayNa/1.0 Android"
                );

                int responseCode =
                        connection.getResponseCode();

                if (responseCode != 200) {

                    throw new Exception(
                            "Overpass response " +
                            responseCode
                    );
                }

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        connection.getInputStream(),
                                        "UTF-8"
                                )
                        );

                StringBuilder result =
                        new StringBuilder();

                String line;

                while (
                        (line = reader.readLine())
                                != null
                ) {

                    result.append(line);
                }

                reader.close();

                JSONObject object =
                        new JSONObject(
                                result.toString()
                        );

                JSONArray elements =
                        object.optJSONArray(
                                "elements"
                        );

                StringBuilder javascript =
                        new StringBuilder();

                javascript.append(
                        "clearPOIs();"
                );

                if (elements != null) {

                    int maximum =
                            Math.min(
                                    elements.length(),
                                    60
                            );

                    for (
                            int i = 0;
                            i < maximum;
                            i++
                    ) {

                        try {

                            JSONObject element =
                                    elements.getJSONObject(i);

                            JSONObject tags =
                                    element.optJSONObject(
                                            "tags"
                                    );

                            if (tags == null) {
                                continue;
                            }

                            String name =
                                    tags.optString(
                                            "name",
                                            ""
                                    );

                            if (name == null
                                    || name.trim().isEmpty()) {

                                continue;
                            }

                            double elementLat =
                                    element.optDouble(
                                            "lat",
                                            Double.NaN
                                    );

                            double elementLng =
                                    element.optDouble(
                                            "lon",
                                            Double.NaN
                                    );

                            /*
                             * Ways/relations return a center.
                             */
                            if (Double.isNaN(elementLat)
                                    || Double.isNaN(elementLng)) {

                                JSONObject center =
                                        element.optJSONObject(
                                                "center"
                                        );

                                if (center != null) {

                                    elementLat =
                                            center.optDouble(
                                                    "lat",
                                                    Double.NaN
                                            );

                                    elementLng =
                                            center.optDouble(
                                                    "lon",
                                                    Double.NaN
                                            );
                                }
                            }

                            if (Double.isNaN(elementLat)
                                    || Double.isNaN(elementLng)) {

                                continue;
                            }

                            String type =
                                    firstNonEmpty(
                                            tags.optString(
                                                    "amenity",
                                                    ""
                                            ),
                                            tags.optString(
                                                    "shop",
                                                    ""
                                            ),
                                            tags.optString(
                                                    "tourism",
                                                    ""
                                            )
                                    );

                            javascript.append(
                                    "addPOI("
                            );

                            javascript.append(
                                    elementLat
                            );

                            javascript.append(",");

                            javascript.append(
                                    elementLng
                            );

                            javascript.append(",");

                            javascript.append(
                                    "'"
                            );

                            javascript.append(
                                    escapeJS(
                                            name
                                    )
                            );

                            javascript.append(
                                    "','"
                            );

                            javascript.append(
                                    escapeJS(
                                            type
                                    )
                            );

                            javascript.append(
                                    "');"
                            );

                        } catch (Exception ignored) {
                        }
                    }
                }

                final String js =
                        javascript.toString();

                runOnUiThread(() -> {

                    if (mapReady
                            && webView != null) {

                        webView.evaluateJavascript(
                                js,
                                null
                        );
                    }

                    loadingNearbyPlaces = false;
                });

            } catch (Exception e) {

                runOnUiThread(() -> {

                    /*
                     * Do not break the map if the
                     * free POI server is unavailable.
                     */
                    loadingNearbyPlaces = false;
                });

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }

    private void searchPlace() {

        String query =
                searchInput
                        .getText()
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
                "🔎 Searching..."
        );

        loading.setPadding(
                10,
                10,
                10,
                10
        );

        searchResultsContainer.addView(
                loading
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
                        "https://photon.komoot.io/api/" +
                        "?q=" +
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

                if (connection.getResponseCode()
                        != 200) {

                    throw new Exception(
                            "Search failed"
                    );
                }

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        connection.getInputStream(),
                                        "UTF-8"
                                )
                        );

                StringBuilder result =
                        new StringBuilder();

                String line;

                while (
                        (line = reader.readLine())
                                != null
                ) {

                    result.append(line);
                }

                reader.close();

                JSONObject object =
                        new JSONObject(
                                result.toString()
                        );

                JSONArray features =
                        object.optJSONArray(
                                "features"
                        );

                runOnUiThread(() -> {

                    searchResultsContainer
                            .removeAllViews();

                    if (features == null
                            || features.length() == 0) {

                        TextView empty =
                                new TextView(this);

                        empty.setText(
                                "No places found."
                        );

                        empty.setPadding(
                                10,
                                10,
                                10,
                                10
                        );

                        searchResultsContainer
                                .addView(empty);

                        return;
                    }

                    for (
                            int i = 0;
                            i < features.length();
                            i++
                    ) {

                        try {

                            JSONObject feature =
                                    features.getJSONObject(
                                            i
                                    );

                            JSONObject properties =
                                    feature.optJSONObject(
                                            "properties"
                                    );

                            JSONObject geometry =
                                    feature.optJSONObject(
                                            "geometry"
                                    );

                            JSONArray coordinates =
                                    geometry == null
                                            ? null
                                            : geometry.optJSONArray(
                                                    "coordinates"
                                            );

                            if (coordinates == null
                                    || coordinates.length()
                                    < 2) {

                                continue;
                            }

                            double lng =
                                    coordinates.optDouble(
                                            0
                                    );

                            double lat =
                                    coordinates.optDouble(
                                            1
                                    );

                            String name =
                                    properties == null
                                            ? ""
                                            : properties.optString(
                                                    "name",
                                                    ""
                                            );

                            String street =
                                    properties == null
                                            ? ""
                                            : properties.optString(
                                                    "street",
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
                                            street,
                                            city,
                                            state
                                    );

                            if (display.isEmpty()) {

                                display =
                                        "Selected place";
                            }

                            Button button =
                                    new Button(this);

                            button.setText(
                                    "📍 " +
                                    display
                            );

                            button.setGravity(
                                    Gravity.LEFT
                            );

                            final double finalLat =
                                    lat;

                            final double finalLng =
                                    lng;

                            final String finalName =
                                    display;

                            button.setOnClickListener(
                                    v ->
                                            reverseGeocode(
                                                    finalLat,
                                                    finalLng,
                                                    finalName
                                            )
                            );

                            searchResultsContainer
                                    .addView(button);

                        } catch (Exception ignored) {
                        }
                    }
                });

            } catch (Exception e) {

                runOnUiThread(() -> {

                    searchResultsContainer
                            .removeAllViews();

                    TextView error =
                            new TextView(this);

                    error.setText(
                            "❌ Search failed. Check internet connection."
                    );

                    error.setTextColor(
                            Color.RED
                    );

                    error.setPadding(
                            10,
                            10,
                            10,
                            10
                    );

                    searchResultsContainer
                            .addView(error);
                });

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }

    private String buildPlaceName(
            String name,
            String street,
            String city,
            String state
    ) {

        StringBuilder result =
                new StringBuilder();

        addPart(result, name);
        addPart(result, street);
        addPart(result, city);
        addPart(result, state);

        return result.toString();
    }

    private void reverseGeocode(
            double lat,
            double lng
    ) {

        reverseGeocode(
                lat,
                lng,
                ""
        );
    }

    private void reverseGeocode(
            double lat,
            double lng,
            String preferredName
    ) {

        statusText.setText(
                "🔎 Finding place name..."
        );

        new Thread(() -> {

            HttpURLConnection connection = null;

            try {

                String urlString =
                        "https://nominatim.openstreetmap.org/reverse" +
                        "?format=jsonv2" +
                        "&lat=" +
                        lat +
                        "&lon=" +
                        lng +
                        "&zoom=18" +
                        "&addressdetails=1" +
                        "&namedetails=1" +
                        "&accept-language=en";

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

                if (connection.getResponseCode()
                        != 200) {

                    throw new Exception(
                            "Reverse lookup failed"
                    );
                }

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        connection.getInputStream(),
                                        "UTF-8"
                                )
                        );

                StringBuilder result =
                        new StringBuilder();

                String line;

                while (
                        (line = reader.readLine())
                                != null
                ) {

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

                JSONObject names =
                        object.optJSONObject(
                                "namedetails"
                        );

                String name =
                        firstNonEmpty(
                                object.optString(
                                        "name",
                                        ""
                                ),
                                preferredName,
                                names == null
                                        ? ""
                                        : names.optString(
                                                "name",
                                                ""
                                        ),
                                names == null
                                        ? ""
                                        : names.optString(
                                                "name:en",
                                                ""
                                        )
                        );

                String house =
                        getAddress(
                                address,
                                "house_number"
                        );

                String road =
                        firstNonEmpty(
                                getAddress(
                                        address,
                                        "road"
                                ),
                                getAddress(
                                        address,
                                        "pedestrian"
                                ),
                                getAddress(
                                        address,
                                        "street"
                                )
                        );

                String barangay =
                        firstNonEmpty(
                                getAddress(
                                        address,
                                        "quarter"
                                ),
                                getAddress(
                                        address,
                                        "locality"
                                ),
                                getAddress(
                                        address,
                                        "village"
                                ),
                                getAddress(
                                        address,
                                        "neighbourhood"
                                ),
                                getAddress(
                                        address,
                                        "suburb"
                                )
                        );

                String city =
                        firstNonEmpty(
                                getAddress(
                                        address,
                                        "city"
                                ),
                                getAddress(
                                        address,
                                        "municipality"
                                ),
                                getAddress(
                                        address,
                                        "town"
                                )
                        );

                String province =
                        firstNonEmpty(
                                getAddress(
                                        address,
                                        "state"
                                ),
                                getAddress(
                                        address,
                                        "province"
                                )
                        );

                String postcode =
                        getAddress(
                                address,
                                "postcode"
                        );

                String country =
                        getAddress(
                                address,
                                "country"
                        );

                String finalName =
                        buildDetailedAddress(
                                name,
                                house,
                                road,
                                barangay,
                                city,
                                province,
                                postcode,
                                country
                        );

                if (finalName.isEmpty()) {

                    finalName =
                            object.optString(
                                    "display_name",
                                    ""
                            );
                }

                if (finalName.isEmpty()) {

                    finalName =
                            String.format(
                                    Locale.US,
                                    "Selected location (%.6f, %.6f)",
                                    lat,
                                    lng
                            );
                }

                final String resultName =
                        finalName;

                runOnUiThread(() ->
                        selectDestination(
                                lat,
                                lng,
                                resultName
                        )
                );

            } catch (Exception e) {

                String fallback =
                        preferredName;

                if (fallback == null
                        || fallback.trim().isEmpty()) {

                    fallback =
                            String.format(
                                    Locale.US,
                                    "Selected location (%.6f, %.6f)",
                                    lat,
                                    lng
                            );
                }

                final String resultName =
                        fallback;

                runOnUiThread(() ->
                        selectDestination(
                                lat,
                                lng,
                                resultName
                        )
                );

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }

    private String getAddress(
            JSONObject address,
            String key
    ) {

        if (address == null) {
            return "";
        }

        String value =
                address.optString(
                        key,
                        ""
                );

        return value == null
                ? ""
                : value.trim();
    }

    private String buildDetailedAddress(
            String name,
            String house,
            String road,
            String barangay,
            String city,
            String province,
            String postcode,
            String country
    ) {

        StringBuilder result =
                new StringBuilder();

        addPart(result, name);

        if (!house.isEmpty()
                && !road.isEmpty()) {

            addPart(
                    result,
                    house + " " + road
            );

        } else {

            addPart(result, house);
            addPart(result, road);
        }

        addPart(result, barangay);
        addPart(result, city);
        addPart(result, province);
        addPart(result, postcode);
        addPart(result, country);

        return result.toString();
    }

    private void addPart(
            StringBuilder result,
            String value
    ) {

        if (value == null
                || value.trim().isEmpty()) {

            return;
        }

        String clean =
                value.trim();

        if (result.length() > 0) {

            String existing =
                    result.toString();

            if (existing.contains(clean)) {
                return;
            }

            result.append(", ");
        }

        result.append(clean);
    }

    private String firstNonEmpty(
            String... values
    ) {

        if (values == null) {
            return "";
        }

        for (String value : values) {

            if (value != null
                    && !value.trim().isEmpty()) {

                return value.trim();
            }
        }

        return "";
    }

    private void selectDestination(
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
                name
        );

        destinationText.setTextColor(
                Color.rgb(0, 110, 70)
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

        if (mapReady) {

            String js =
                    "setDestination(" +
                    lat +
                    "," +
                    lng +
                    ",'" +
                    escapeJS(name) +
                    "');";

            webView.evaluateJavascript(
                    js,
                    null
            );
        }

        statusText.setText(
                "✅ Destination selected"
        );
    }

    private String escapeJS(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .replace(
                        "\\",
                        "\\\\"
                )
                .replace(
                        "'",
                        "\\'"
                )
                .replace(
                        "\n",
                        " "
                )
                .replace(
                        "\r",
                        " "
                );
    }

    private void returnDestination() {

        if (!destinationChosen) {

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

        if (rideId.isEmpty()) {

            statusText.setText(
                    "❌ No active ride."
            );

            return;
        }

        rideListener =
                db.collection("rides")
                        .document(rideId)
                        .addSnapshotListener(
                                (snapshot, error) -> {

                                    if (error != null) {

                                        statusText.setText(
                                                "❌ Ride connection error"
                                        );

                                        return;
                                    }

                                    if (snapshot == null
                                            || !snapshot.exists()) {

                                        statusText.setText(
                                                "❌ Ride not found"
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
                value(
                        ride,
                        "status"
                );

        String pickup =
                value(
                        ride,
                        "pickupName"
                );

        if (pickup.isEmpty()) {

            pickup =
                    value(
                            ride,
                            "pickup"
                    );
        }

        String destination =
                value(
                        ride,
                        "destinationName"
                );

        if (destination.isEmpty()) {

            destination =
                    value(
                            ride,
                            "destination"
                    );
        }

        statusText.setText(
                "🟢 LIVE RIDE\n" +
                "Status: " +
                status +
                "\n📍 " +
                pickup +
                "\n🏁 " +
                destination
        );

        String driverId =
                value(
                        ride,
                        "driverId"
                );

        if (!driverId.isEmpty()) {

            listenToDriver(
                    driverId
            );
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

                            if (lat != null
                                    && lng != null
                                    && mapReady) {

                                webView.evaluateJavascript(
                                        "setDriver(" +
                                        lat +
                                        "," +
                                        lng +
                                        ");",
                                        null
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
                    && grantResults[0]
                    == PackageManager.PERMISSION_GRANTED) {

                startLocationTracking();

            } else {

                statusText.setText(
                        "❌ Location permission denied."
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

            } catch (Exception ignored) {
            }
        }

        if (rideListener != null) {
            rideListener.remove();
            rideListener = null;
        }

        if (driverListener != null) {
            driverListener.remove();
            driverListener = null;
        }

        if (webView != null) {

            webView.stopLoading();
            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }
}
