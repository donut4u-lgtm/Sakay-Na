package com.sakyna.app;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.location.Address;
import android.location.Geocoder;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BookingActivityMapActivity extends Activity {

    private static final String TAG = "SakayNaBookingMap";
    private static final String ALL_PROVINCES = "All Provinces";
    private static final String ALL_TOWNS = "All Towns / Cities";

    private static final int GREEN = Color.rgb(0, 125, 75);
    private static final int DARK_GREEN = Color.rgb(0, 95, 55);

    private FirebaseFirestore db;
    private ListenerRegistration ridesListener;

    private LinearLayout root;
    private Spinner provinceSpinner;
    private Spinner townSpinner;
    private WebView mapView;

    private TextView statusText;
    private TextView dateText;
    private TextView detailsText;

    private Button startDateButton;
    private Button endDateButton;

    private final List<RideItem> rides = new ArrayList<>();
    private final List<AreaPlace> areaPlaces = new ArrayList<>();
    private final List<String> provinceNames = new ArrayList<>();

    private final ExecutorService background =
            Executors.newFixedThreadPool(2);

    private long selectedStart;
    private long selectedEnd;

    private boolean updatingSpinners;
    private boolean mapPageReady;
    private boolean activityDestroyed;
    private boolean areaLoadFinished;

    private static class RideItem {

        String id;
        String passengerId;
        String status;
        String pickup;
        String destination;
        String paymentMethod;
        String paymentStatus;

        String province = "";
        String town = "";

        double pickupLat;
        double pickupLng;
        double destinationLat;
        double destinationLng;

        long createdAt;
        boolean locationResolved;

        RideItem(String id) {
            this.id = id;
        }
    }

    private static class AreaPlace {

        String name;
        String province;
        String type;

        AreaPlace(String name, String province, String type) {
            this.name = name;
            this.province = province;
            this.type = type;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Calendar calendar = Calendar.getInstance();
            setStartOfDay(calendar);

            selectedStart = calendar.getTimeInMillis();

            calendar.add(Calendar.DAY_OF_MONTH, 1);
            selectedEnd = calendar.getTimeInMillis() - 1;

            db = FirebaseFirestore.getInstance();

            buildScreen();
            setupMap();
            setupSpinners();
            loadPhilippineAreas();
            listenForBookings();

        } catch (Exception e) {
            Log.e(TAG, "Booking map startup failed", e);
            showStartupError(e);
        }
    }

    private void showStartupError(Exception e) {
        try {
            TextView message = new TextView(this);

            message.setText(
                    "SAKAY NA BOOKING MAP\n\n" +
                    "The booking map could not start.\n\n" +
                    "Error: " + e.getClass().getSimpleName() +
                    "\n\nDetails: " + safe(e.getMessage())
            );

            message.setTextSize(16);
            message.setTextColor(Color.DKGRAY);
            message.setPadding(24, 32, 24, 32);

            LinearLayout screen = new LinearLayout(this);
            screen.setOrientation(LinearLayout.VERTICAL);
            screen.setPadding(16, 16, 16, 16);

            screen.addView(
                    message,
                    new LinearLayout.LayoutParams(-1, 0, 1)
            );

            Button back = new Button(this);
            back.setText("BACK TO ADMIN DASHBOARD");
            back.setOnClickListener(v -> finish());

            screen.addView(
                    back,
                    new LinearLayout.LayoutParams(-1, -2)
            );

            setContentView(screen);

        } catch (Exception ignored) {
            finish();
        }
    }

    private int dp(float value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }

    private void setStartOfDay(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private void buildScreen() {

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(245, 248, 246));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER);
        header.setPadding(dp(8), dp(10), dp(8), dp(10));
        header.setBackgroundColor(GREEN);

        TextView title = new TextView(this);
        title.setText("SAKAY NA");
        title.setTextColor(Color.WHITE);
        title.setTextSize(25);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, android.graphics.Typeface.BOLD);

        header.addView(
                title,
                new LinearLayout.LayoutParams(-1, -2)
        );

        TextView subtitle = new TextView(this);
        subtitle.setText("LIVE BOOKING ACTIVITY MAP");
        subtitle.setTextColor(Color.WHITE);
        subtitle.setTextSize(16);
        subtitle.setGravity(Gravity.CENTER);

        header.addView(
                subtitle,
                new LinearLayout.LayoutParams(-1, -2)
        );

        root.addView(
                header,
                new LinearLayout.LayoutParams(-1, -2)
        );

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(8), dp(4), dp(8), dp(8));

        statusText = new TextView(this);
        statusText.setText(
                "Connecting to live bookings and loading Philippine areas..."
        );
        statusText.setTextSize(14);
        statusText.setTextColor(DARK_GREEN);
        statusText.setPadding(dp(2), dp(4), dp(2), dp(4));

        content.addView(statusText);

        content.addView(label("PROVINCE"));

        provinceSpinner = new Spinner(this);

        content.addView(
                provinceSpinner,
                new LinearLayout.LayoutParams(-1, dp(44))
        );

        content.addView(label("TOWN / CITY"));

        townSpinner = new Spinner(this);

        content.addView(
                townSpinner,
                new LinearLayout.LayoutParams(-1, dp(44))
        );

        content.addView(label("BOOKING DATE RANGE"));

        LinearLayout dateRow = new LinearLayout(this);
        dateRow.setOrientation(LinearLayout.HORIZONTAL);

        startDateButton = new Button(this);
        endDateButton = new Button(this);

        startDateButton.setOnClickListener(
                v -> showDatePicker(true)
        );

        endDateButton.setOnClickListener(
                v -> showDatePicker(false)
        );

        dateRow.addView(
                startDateButton,
                new LinearLayout.LayoutParams(0, dp(44), 1)
        );

        dateRow.addView(
                endDateButton,
                new LinearLayout.LayoutParams(0, dp(44), 1)
        );

        content.addView(dateRow);

        dateText = new TextView(this);
        dateText.setTextSize(13);
        dateText.setTextColor(Color.DKGRAY);
        dateText.setPadding(dp(2), dp(2), dp(2), dp(2));

        content.addView(dateText);

        Button todayButton = new Button(this);
        todayButton.setText("SHOW TODAY'S BOOKINGS");
        todayButton.setTextColor(Color.WHITE);
        todayButton.setBackgroundColor(GREEN);

        todayButton.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            setStartOfDay(calendar);

            selectedStart = calendar.getTimeInMillis();

            calendar.add(Calendar.DAY_OF_MONTH, 1);
            selectedEnd = calendar.getTimeInMillis() - 1;

            updateDateText();
            renderMap();
        });

        content.addView(
                todayButton,
                new LinearLayout.LayoutParams(-1, dp(46))
        );

        content.addView(
                label("PICKUP AND DESTINATION LOCATIONS")
        );

        mapView = new WebView(this);

        int screenHeight =
                getResources().getDisplayMetrics().heightPixels;

        int mapHeight = Math.max(
                dp(300),
                (int) (screenHeight * 0.46f)
        );

        mapView.setLayoutParams(
                new LinearLayout.LayoutParams(-1, mapHeight)
        );

        content.addView(mapView);

        content.addView(
                label("SELECTED BOOKING DETAILS")
        );

        detailsText = new TextView(this);
        detailsText.setText(
                "Tap a pickup or destination marker on the map."
        );
        detailsText.setTextColor(Color.DKGRAY);
        detailsText.setTextSize(14);
        detailsText.setPadding(dp(8), dp(8), dp(8), dp(8));
        detailsText.setBackgroundColor(Color.WHITE);

        content.addView(
                detailsText,
                new LinearLayout.LayoutParams(-1, -2)
        );

        Button backButton = new Button(this);
        backButton.setText("BACK TO ADMIN DASHBOARD");
        backButton.setOnClickListener(v -> finish());

        content.addView(
                backButton,
                new LinearLayout.LayoutParams(-1, dp(46))
        );

        scroll.addView(content);

        root.addView(
                scroll,
                new LinearLayout.LayoutParams(-1, 0, 1)
        );

        setContentView(root);

        updateDateText();
    }

    private TextView label(String text) {

        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(14);
        view.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );
        view.setTextColor(DARK_GREEN);
        view.setPadding(dp(2), dp(5), dp(2), dp(1));

        return view;
    }

    private void setupMap() {

        WebSettings settings = mapView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        mapView.setBackgroundColor(Color.rgb(235, 241, 237));

        String html =
                "<!DOCTYPE html><html><head>" +
                "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
                "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>" +
                "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>" +
                "<style>" +
                "html,body,#map{height:100%;width:100%;margin:0;padding:0;}" +
                ".leaflet-popup-content{font-size:14px;}" +
                "</style></head><body>" +
                "<div id='map'></div><script>" +

                "var map=L.map('map').setView([12.8797,121.7740],6);" +

                "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'," +
                "{maxZoom:19,attribution:'&copy; OpenStreetMap contributors'}).addTo(map);" +

                "var markers=[];" +

                "function clearMarkers(){" +
                "markers.forEach(function(m){map.removeLayer(m);});markers=[];}" +

                "function addBookingMarkers(data){" +
                "clearMarkers();var points=[];" +
                "data.forEach(function(r){" +

                "var p=L.marker([r.plat,r.plng]).addTo(map);" +

                "p.bindPopup('<b>PICKUP</b><br>'+r.pickup+" +
                "'<br><b>Status:</b> '+r.status+" +
                "'<br><button onclick=\"showDetails(\\''+r.id+'\\')\">Booking details</button>');" +

                "p.on('click',function(){showDetails(r.id);});" +
                "markers.push(p);points.push([r.plat,r.plng]);" +

                "var d=L.marker([r.dlat,r.dlng]).addTo(map);" +

                "d.bindPopup('<b>DESTINATION</b><br>'+r.destination+" +
                "'<br><b>Status:</b> '+r.status+" +
                "'<br><button onclick=\"showDetails(\\''+r.id+'\\')\">Booking details</button>');" +

                "d.on('click',function(){showDetails(r.id);});" +
                "markers.push(d);points.push([r.dlat,r.dlng]);" +
                "});" +

                "if(points.length){map.fitBounds(points,{padding:[25,25],maxZoom:15});}" +
                "}" +

                "function showDetails(id){" +
                "window.location.href='sakayna://booking/'+encodeURIComponent(id);" +
                "}" +

                "</script></body></html>";

        mapView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                if (activityDestroyed) return;

                mapPageReady = true;
                renderMap();

                view.postDelayed(() -> {
                    if (!activityDestroyed) {
                        view.evaluateJavascript(
                                "map.invalidateSize()",
                                null
                        );
                    }
                }, 250);
            }

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    String url
            ) {

                if (url != null &&
                        url.startsWith("sakayna://booking/")) {

                    try {
                        String id = URLDecoder.decode(
                                url.substring(
                                        "sakayna://booking/".length()
                                ),
                                "UTF-8"
                        );

                        showBookingDetails(id);

                    } catch (Exception e) {
                        Log.e(TAG, "Unable to open booking details", e);
                    }

                    return true;
                }

                return false;
            }
        });

        mapView.loadDataWithBaseURL(
                "https://localhost/",
                html,
                "text/html",
                "UTF-8",
                null
        );
    }

    private void setupSpinners() {

        provinceSpinner.setAdapter(
                adapter(Collections.singletonList(ALL_PROVINCES))
        );

        townSpinner.setAdapter(
                adapter(Collections.singletonList(ALL_TOWNS))
        );

        provinceSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {
                        if (updatingSpinners || activityDestroyed) {
                            return;
                        }

                        refreshTownSpinner();
                        renderMap();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                }
        );

        townSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {
                        if (!updatingSpinners && !activityDestroyed) {
                            renderMap();
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                }
        );
    }

    private ArrayAdapter<String> adapter(List<String> values) {

        ArrayAdapter<String> result = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                values
        );

        result.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        return result;
    }

    /*
     * Loads Philippine provinces and cities/municipalities independently
     * of Firestore. This means the dropdowns can work before any booking
     * exists in the database.
     */
    private void loadPhilippineAreas() {

        statusText.setText(
                "Loading Philippine provinces and cities/municipalities..."
        );

        try {
            background.execute(() -> {

                try {
                    JSONArray provincesJson = fetchArray(
                            "https://psgc.cloud/api/v2/provinces"
                    );

                    JSONArray placesJson = fetchArray(
                            "https://psgc.cloud/api/v2/cities-municipalities"
                    );

                    List<String> loadedProvinces = new ArrayList<>();
                    loadedProvinces.add(ALL_PROVINCES);

                    for (int i = 0; i < provincesJson.length(); i++) {

                        JSONObject object =
                                provincesJson.optJSONObject(i);

                        if (object == null) continue;

                        String name = jsonName(object.opt("name"));

                        if (!name.isEmpty()) {
                            loadedProvinces.add(name);
                        }
                    }

                    Collections.sort(
                            loadedProvinces.subList(
                                    1,
                                    loadedProvinces.size()
                            ),
                            String.CASE_INSENSITIVE_ORDER
                    );

                    List<AreaPlace> loadedPlaces = new ArrayList<>();

                    for (int i = 0; i < placesJson.length(); i++) {

                        JSONObject object =
                                placesJson.optJSONObject(i);

                        if (object == null) continue;

                        String name = jsonName(object.opt("name"));
                        String province = jsonName(object.opt("province"));
                        String type = jsonName(object.opt("type"));

                        if (!name.isEmpty()) {
                            loadedPlaces.add(
                                    new AreaPlace(name, province, type)
                            );
                        }
                    }

                    runOnUiThread(() -> {

                        if (activityDestroyed) return;

                        provinceNames.clear();
                        provinceNames.addAll(loadedProvinces);

                        areaPlaces.clear();
                        areaPlaces.addAll(loadedPlaces);

                        areaLoadFinished = true;

                        String previousProvince = selectedProvince();

                        updatingSpinners = true;

                        provinceSpinner.setAdapter(
                                adapter(provinceNames)
                        );

                        int index = findIgnoreCase(
                                provinceNames,
                                previousProvince
                        );

                        provinceSpinner.setSelection(
                                Math.max(0, index)
                        );

                        updatingSpinners = false;

                        refreshTownSpinner();
                        renderMap();

                        statusText.setText(
                                "Philippine area list loaded • Live booking records: "
                                        + rides.size()
                        );
                    });

                } catch (Exception e) {

                    Log.e(TAG, "Unable to load Philippine area list", e);

                    runOnUiThread(() -> {

                        if (activityDestroyed) return;

                        areaLoadFinished = true;
                        rebuildLocationFilters();

                        statusText.setText(
                                "Area list could not load. Check internet. " +
                                "Showing areas found in bookings, if available."
                        );
                    });
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Unable to start area-list request", e);
        }
    }

    private JSONArray fetchArray(String address) throws Exception {

        HttpURLConnection connection =
                (HttpURLConnection) new URL(address).openConnection();

        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(15000);
        connection.setRequestProperty("Accept", "application/json");

        try {
            int code = connection.getResponseCode();

            if (code < 200 || code >= 300) {
                throw new Exception("Area API returned HTTP " + code);
            }

            java.io.InputStream input = connection.getInputStream();
            java.io.ByteArrayOutputStream output =
                    new java.io.ByteArrayOutputStream();

            byte[] buffer = new byte[4096];
            int count;

            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }

            input.close();

            String body = output.toString("UTF-8");

            Object parsed =
                    new org.json.JSONTokener(body).nextValue();

            if (parsed instanceof JSONArray) {
                return (JSONArray) parsed;
            }

            if (parsed instanceof JSONObject) {

                JSONObject object = (JSONObject) parsed;

                JSONArray data = object.optJSONArray("data");

                if (data != null) return data;

                JSONArray results = object.optJSONArray("results");

                if (results != null) return results;
            }

            throw new Exception("Unexpected area-list response");

        } finally {
            connection.disconnect();
        }
    }

    private String jsonName(Object value) {

        if (value == null || value == JSONObject.NULL) {
            return "";
        }

        if (value instanceof JSONObject) {

            JSONObject object = (JSONObject) value;

            String name = safe(object.optString("name", ""));

            if (name.isEmpty()) {
                name = safe(object.optString("provinceName", ""));
            }

            return name;
        }

        String result = String.valueOf(value).trim();

        return "null".equalsIgnoreCase(result) ? "" : result;
    }

    private int findIgnoreCase(List<String> list, String value) {

        for (int i = 0; i < list.size(); i++) {

            if (list.get(i).equalsIgnoreCase(value)) {
                return i;
            }
        }

        return -1;
    }

    private void listenForBookings() {

        ridesListener = db.collection("rides")
                .addSnapshotListener((snapshot, error) -> {

                    if (activityDestroyed || isFinishing()) return;

                    if (error != null) {

                        statusText.setText(
                                "Unable to load live bookings. " +
                                "Check connection and Firestore access."
                        );

                        Log.e(TAG, "Live booking listener failed", error);
                        return;
                    }

                    if (snapshot == null) return;

                    try {
                        List<RideItem> updatedRides = new ArrayList<>();

                        for (DocumentSnapshot doc : snapshot.getDocuments()) {

                            RideItem ride = readRide(doc);

                            if (ride == null) continue;

                            for (RideItem oldRide : rides) {

                                if (oldRide.id.equals(ride.id)) {

                                    if (ride.province.isEmpty()) {
                                        ride.province = oldRide.province;
                                    }

                                    if (ride.town.isEmpty()) {
                                        ride.town = oldRide.town;
                                    }

                                    ride.locationResolved =
                                            oldRide.locationResolved;

                                    break;
                                }
                            }

                            updatedRides.add(ride);
                        }

                        rides.clear();
                        rides.addAll(updatedRides);

                        rebuildLocationFilters();
                        renderMap();
                        resolveUnresolvedLocations();

                        if (areaLoadFinished) {
                            statusText.setText(
                                    "Philippine area list ready • Live booking records: "
                                            + rides.size()
                            );
                        }

                    } catch (Exception e) {

                        Log.e(TAG, "Unable to process booking records", e);

                        statusText.setText(
                                "Bookings were received, but could not be displayed."
                        );
                    }
                });
    }

    private RideItem readRide(DocumentSnapshot doc) {

        Double pickupLat = getNumber(doc, "pickupLatitude");
        Double pickupLng = getNumber(doc, "pickupLongitude");
        Double destinationLat = getNumber(doc, "destinationLatitude");
        Double destinationLng = getNumber(doc, "destinationLongitude");

        if (pickupLat == null ||
                pickupLng == null ||
                destinationLat == null ||
                destinationLng == null) {
            return null;
        }

        if (!validCoordinates(pickupLat, pickupLng) ||
                !validCoordinates(destinationLat, destinationLng)) {
            return null;
        }

        RideItem ride = new RideItem(doc.getId());

        ride.pickupLat = pickupLat;
        ride.pickupLng = pickupLng;
        ride.destinationLat = destinationLat;
        ride.destinationLng = destinationLng;

        ride.passengerId = safe(doc.getString("passengerId"));
        ride.status = safe(doc.getString("status"));

        ride.pickup = firstNonEmpty(
                doc.getString("pickupName"),
                doc.getString("pickup")
        );

        ride.destination = firstNonEmpty(
                doc.getString("destinationName"),
                doc.getString("destination")
        );

        ride.paymentMethod = safe(doc.getString("paymentMethod"));
        ride.paymentStatus = safe(doc.getString("paymentStatus"));

        /*
         * If area information is already stored in the ride document,
         * use it before trying reverse geocoding.
         */
        ride.province = firstUseful(
                doc,
                "pickupProvince",
                "province",
                "provinceName"
        );

        ride.town = firstUseful(
                doc,
                "pickupTown",
                "pickupCity",
                "town",
                "city",
                "municipality",
                "pickupMunicipality"
        );

        ride.locationResolved =
                !ride.province.isEmpty() && !ride.town.isEmpty();

        Timestamp created = doc.getTimestamp("createdAt");

        if (created != null) {
            ride.createdAt = created.toDate().getTime();
        }

        return ride;
    }

    private String firstUseful(
            DocumentSnapshot doc,
            String... keys
    ) {

        for (String key : keys) {

            Object value = doc.get(key);

            if (value instanceof String &&
                    !((String) value).trim().isEmpty()) {

                return ((String) value).trim();
            }
        }

        return "";
    }

    private Double getNumber(DocumentSnapshot doc, String key) {

        Object value = doc.get(key);

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        return null;
    }

    private boolean validCoordinates(double lat, double lng) {

        return lat >= -90 &&
                lat <= 90 &&
                lng >= -180 &&
                lng <= 180 &&
                !(lat == 0 && lng == 0);
    }

    private void resolveUnresolvedLocations() {

        if (activityDestroyed || !Geocoder.isPresent()) {
            return;
        }

        for (RideItem ride : new ArrayList<>(rides)) {

            if (ride.locationResolved) continue;

            ride.locationResolved = true;

            try {
                background.execute(() -> {

                    String province = "";
                    String town = "";

                    try {
                        Geocoder geocoder = new Geocoder(
                                getApplicationContext(),
                                Locale.getDefault()
                        );

                        List<Address> addresses =
                                geocoder.getFromLocation(
                                        ride.pickupLat,
                                        ride.pickupLng,
                                        1
                                );

                        if (addresses != null && !addresses.isEmpty()) {

                            Address address = addresses.get(0);

                            province = safe(address.getAdminArea());

                            town = firstNonEmpty(
                                    address.getLocality(),
                                    address.getSubAdminArea(),
                                    address.getSubLocality()
                            );

                            if ("Not available".equals(town)) {
                                town = "";
                            }
                        }

                    } catch (Exception e) {
                        Log.w(
                                TAG,
                                "Address lookup failed for " + ride.id,
                                e
                        );
                    }

                    final String resolvedProvince = province;
                    final String resolvedTown = town;

                    runOnUiThread(() -> {

                        if (activityDestroyed || isFinishing()) return;

                        for (RideItem current : rides) {

                            if (current.id.equals(ride.id)) {

                                if (current.province.isEmpty()) {
                                    current.province = resolvedProvince;
                                }

                                if (current.town.isEmpty()) {
                                    current.town = resolvedTown;
                                }

                                break;
                            }
                        }

                        rebuildLocationFilters();
                        renderMap();
                    });
                });

            } catch (Exception e) {
                Log.w(TAG, "Unable to schedule address lookup", e);
            }
        }
    }

    private void rebuildLocationFilters() {

        if (activityDestroyed ||
                provinceSpinner == null ||
                townSpinner == null) {
            return;
        }

        /*
         * When the national area list has loaded, don't replace it
         * with areas found in bookings.
         */
        if (areaLoadFinished && !provinceNames.isEmpty()) {
            refreshTownSpinner();
            return;
        }

        String previousProvince = selectedProvince();

        LinkedHashSet<String> values = new LinkedHashSet<>();
        values.add(ALL_PROVINCES);

        for (RideItem ride : rides) {

            if (!ride.province.isEmpty()) {
                values.add(ride.province);
            }
        }

        List<String> list = new ArrayList<>(values);

        Collections.sort(
                list.subList(1, list.size()),
                String.CASE_INSENSITIVE_ORDER
        );

        updatingSpinners = true;

        provinceSpinner.setAdapter(adapter(list));

        provinceSpinner.setSelection(
                Math.max(0, findIgnoreCase(list, previousProvince))
        );

        updatingSpinners = false;

        refreshTownSpinner();
    }

    private void refreshTownSpinner() {

        if (activityDestroyed ||
                townSpinner == null ||
                provinceSpinner == null) {
            return;
        }

        String province = selectedProvince();
        String previousTown = selectedTown();

        LinkedHashSet<String> values = new LinkedHashSet<>();
        values.add(ALL_TOWNS);

        if (areaLoadFinished && !areaPlaces.isEmpty()) {

            for (AreaPlace place : areaPlaces) {

                if (ALL_PROVINCES.equals(province) ||
                        sameArea(place.province, province)) {

                    values.add(place.name);
                }
            }

        } else {

            for (RideItem ride : rides) {

                if ((ALL_PROVINCES.equals(province) ||
                        sameArea(ride.province, province)) &&
                        !ride.town.isEmpty()) {

                    values.add(ride.town);
                }
            }
        }

        List<String> list = new ArrayList<>(values);

        Collections.sort(
                list.subList(1, list.size()),
                String.CASE_INSENSITIVE_ORDER
        );

        updatingSpinners = true;

        townSpinner.setAdapter(adapter(list));

        townSpinner.setSelection(
                Math.max(0, findIgnoreCase(list, previousTown))
        );

        updatingSpinners = false;
    }

    private boolean sameArea(String first, String second) {
        return normalizeArea(first).equals(normalizeArea(second));
    }

    private String normalizeArea(String value) {

        if (value == null) return "";

        String result = value.toLowerCase(Locale.ROOT).trim();

        result = result.replace("province of ", "");
        result = result.replace("city of ", "");
        result = result.replace(" city", "");
        result = result.replace(" municipality", "");
        result = result.replace(" (capital)", "");

        return result.replaceAll("[^a-z0-9]", "");
    }

    private String selectedProvince() {

        Object selected = provinceSpinner == null
                ? null
                : provinceSpinner.getSelectedItem();

        return selected == null
                ? ALL_PROVINCES
                : selected.toString();
    }

    private String selectedTown() {

        Object selected = townSpinner == null
                ? null
                : townSpinner.getSelectedItem();

        return selected == null
                ? ALL_TOWNS
                : selected.toString();
    }

    private void showDatePicker(boolean startDate) {

        if (activityDestroyed) return;

        Calendar calendar = Calendar.getInstance();

        calendar.setTimeInMillis(
                startDate ? selectedStart : selectedEnd
        );

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (picker, year, month, day) -> {

                    if (activityDestroyed) return;

                    Calendar chosen = Calendar.getInstance();

                    chosen.set(year, month, day, 0, 0, 0);
                    chosen.set(Calendar.MILLISECOND, 0);

                    long newStart = selectedStart;
                    long newEnd = selectedEnd;

                    if (startDate) {
                        newStart = chosen.getTimeInMillis();
                    } else {
                        chosen.add(Calendar.DAY_OF_MONTH, 1);
                        newEnd = chosen.getTimeInMillis() - 1;
                    }

                    if (newEnd < newStart) {

                        Toast.makeText(
                                this,
                                "End date must not be before start date.",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    selectedStart = newStart;
                    selectedEnd = newEnd;

                    updateDateText();
                    renderMap();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

    private void updateDateText() {

        if (dateText == null ||
                startDateButton == null ||
                endDateButton == null) {
            return;
        }

        SimpleDateFormat format = new SimpleDateFormat(
                "dd MMM yyyy",
                Locale.getDefault()
        );

        dateText.setText(
                "Selected: " +
                format.format(new Date(selectedStart)) +
                " – " +
                format.format(new Date(selectedEnd))
        );

        startDateButton.setText(
                format.format(new Date(selectedStart))
        );

        endDateButton.setText(
                format.format(new Date(selectedEnd))
        );
    }

    private void renderMap() {

        if (activityDestroyed ||
                mapView == null ||
                !mapPageReady) {
            return;
        }

        try {
            String province = selectedProvince();
            String town = selectedTown();

            JSONArray array = new JSONArray();
            int visible = 0;

            for (RideItem ride : rides) {

                if (ride.createdAt < selectedStart ||
                        ride.createdAt > selectedEnd) {
                    continue;
                }

                if (!ALL_PROVINCES.equals(province) &&
                        !sameArea(province, ride.province)) {
                    continue;
                }

                if (!ALL_TOWNS.equals(town) &&
                        !sameArea(town, ride.town)) {
                    continue;
                }

                JSONObject item = new JSONObject();

                item.put("id", ride.id);
                item.put("status", ride.status);
                item.put("pickup", ride.pickup);
                item.put("destination", ride.destination);

                item.put("plat", ride.pickupLat);
                item.put("plng", ride.pickupLng);

                item.put("dlat", ride.destinationLat);
                item.put("dlng", ride.destinationLng);

                array.put(item);
                visible++;
            }

            statusText.setText(
                    "Live booking records: " +
                    rides.size() +
                    " • Matching bookings: " +
                    visible
            );

            String script =
                    "addBookingMarkers(" + array.toString() + ")";

            mapView.post(() -> {

                if (!activityDestroyed &&
                        mapView != null &&
                        mapPageReady) {

                    mapView.evaluateJavascript(script, null);
                }
            });

        } catch (Exception e) {

            Log.e(TAG, "Unable to render booking map", e);

            if (statusText != null) {
                statusText.setText(
                        "Unable to display map records."
                );
            }
        }
    }

    private void showBookingDetails(String id) {

        if (activityDestroyed || detailsText == null) return;

        for (RideItem ride : rides) {

            if (!ride.id.equals(id)) continue;

            SimpleDateFormat format = new SimpleDateFormat(
                    "dd MMM yyyy, hh:mm a",
                    Locale.getDefault()
            );

            detailsText.setText(
                    "BOOKING ID\n" + ride.id +
                    "\n\nSTATUS\n" + ride.status +
                    "\n\nPICKUP\n" + ride.pickup +
                    "\n\nDESTINATION\n" + ride.destination +
                    "\n\nPASSENGER ID\n" + ride.passengerId +
                    "\n\nPAYMENT METHOD\n" + ride.paymentMethod +
                    "\n\nPAYMENT STATUS\n" + ride.paymentStatus +
                    "\n\nBOOKING DATE\n" +
                    (ride.createdAt > 0
                            ? format.format(new Date(ride.createdAt))
                            : "Not available")
            );

            return;
        }

        detailsText.setText(
                "Booking details are no longer available."
        );
    }

    private String firstNonEmpty(String... values) {

        if (values != null) {

            for (String value : values) {

                if (value != null &&
                        !value.trim().isEmpty()) {

                    return value.trim();
                }
            }
        }

        return "Not available";
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    protected void onDestroy() {

        activityDestroyed = true;
        mapPageReady = false;

        if (ridesListener != null) {
            ridesListener.remove();
            ridesListener = null;
        }

        background.shutdownNow();

        if (mapView != null) {

            try {
                mapView.stopLoading();
                mapView.setWebViewClient(null);
                mapView.destroy();

            } catch (Exception e) {
                Log.w(TAG, "WebView cleanup failed", e);
            }

            mapView = null;
        }

        super.onDestroy();
    }
}
