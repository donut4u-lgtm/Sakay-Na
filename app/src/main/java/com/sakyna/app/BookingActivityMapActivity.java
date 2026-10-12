package com.sakyna.app;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
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

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    private final Map<String, String> provinceNameByCode = new HashMap<>();

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

        boolean hasPickupCoordinates;
        boolean hasDestinationCoordinates;
        boolean locationResolutionStarted;

        RideItem(String id) {
            this.id = id;
        }
    }

    private static class AreaPlace {
        String name;
        String province;
        String provinceCode;
        String type;

        AreaPlace(
                String name,
                String province,
                String provinceCode,
                String type
        ) {
            this.name = name;
            this.province = province;
            this.provinceCode = provinceCode;
            this.type = type;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Calendar c = Calendar.getInstance();
            setStartOfDay(c);

            selectedStart = c.getTimeInMillis();

            c.add(Calendar.DAY_OF_MONTH, 1);
            selectedEnd = c.getTimeInMillis() - 1;

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
            TextView msg = new TextView(this);

            msg.setText(
                    "SAKAY NA BOOKING MAP\n\n" +
                    "The booking map could not start.\n\nError: " +
                    e.getClass().getSimpleName() +
                    "\n" + safe(e.getMessage())
            );

            msg.setTextSize(16);
            msg.setTextColor(Color.DKGRAY);
            msg.setPadding(24, 32, 24, 32);

            LinearLayout screen = new LinearLayout(this);
            screen.setOrientation(LinearLayout.VERTICAL);
            screen.setPadding(16, 16, 16, 16);

            screen.addView(
                    msg,
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

    private void setStartOfDay(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
    }

    private void setEndOfDay(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
    }

    private int dp(float value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
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

        LinearLayout dates = new LinearLayout(this);
        dates.setOrientation(LinearLayout.HORIZONTAL);

        startDateButton = new Button(this);
        endDateButton = new Button(this);

        startDateButton.setOnClickListener(v -> showDatePicker(true));
        endDateButton.setOnClickListener(v -> showDatePicker(false));

        dates.addView(
                startDateButton,
                new LinearLayout.LayoutParams(0, dp(44), 1)
        );

        dates.addView(
                endDateButton,
                new LinearLayout.LayoutParams(0, dp(44), 1)
        );

        content.addView(dates);

        dateText = new TextView(this);
        dateText.setTextSize(13);
        dateText.setTextColor(Color.DKGRAY);
        dateText.setPadding(dp(2), dp(2), dp(2), dp(2));
        content.addView(dateText);

        Button today = new Button(this);
        today.setText("SHOW TODAY'S BOOKINGS");
        today.setTextColor(Color.WHITE);
        today.setBackgroundColor(GREEN);

        today.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            setStartOfDay(c);

            selectedStart = c.getTimeInMillis();

            setEndOfDay(c);
            selectedEnd = c.getTimeInMillis();

            updateDateText();
            renderMap();
        });

        content.addView(
                today,
                new LinearLayout.LayoutParams(-1, dp(46))
        );

        content.addView(label("PICKUP AND DESTINATION LOCATIONS"));

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

        content.addView(label("SELECTED BOOKING DETAILS"));

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

        Button back = new Button(this);
        back.setText("BACK TO ADMIN DASHBOARD");
        back.setOnClickListener(v -> finish());

        content.addView(
                back,
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
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(14);
        v.setTypeface(null, android.graphics.Typeface.BOLD);
        v.setTextColor(DARK_GREEN);
        v.setPadding(dp(2), dp(5), dp(2), dp(1));
        return v;
    }

    private void setupMap() {
        WebSettings s = mapView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);

        mapView.setBackgroundColor(Color.rgb(235, 241, 237));

        String html =
                "<!DOCTYPE html><html><head>" +
                "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
                "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>" +
                "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>" +
                "<style>html,body,#map{height:100%;width:100%;margin:0;padding:0}" +
                ".leaflet-popup-content{font-size:14px}</style>" +
                "</head><body><div id='map'></div><script>" +
                "var map=L.map('map').setView([12.8797,121.7740],6);" +
                "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'," +
                "{maxZoom:19,attribution:'&copy; OpenStreetMap contributors'}).addTo(map);" +
                "var markers=[];" +
                "function clearMarkers(){markers.forEach(function(m){map.removeLayer(m)});markers=[];}" +
                "function esc(v){return String(v==null?'':v)" +
                ".replace(/&/g,'&amp;').replace(/</g,'&lt;')" +
                ".replace(/>/g,'&gt;').replace(/\\\"/g,'&quot;')" +
                ".replace(/'/g,'&#39;');}" +
                "function addBookingMarkers(data){clearMarkers();var points=[];" +
                "data.forEach(function(r){" +
                "if(!isFinite(r.plat)||!isFinite(r.plng)||!isFinite(r.dlat)||!isFinite(r.dlng))return;" +
                "var p=L.marker([r.plat,r.plng]).addTo(map);" +
                "p.bindPopup('<b>PICKUP</b><br>'+esc(r.pickup)+'<br><b>Status:</b> '+esc(r.status));" +
                "p.on('click',function(){showDetails(r.id)});" +
                "markers.push(p);points.push([r.plat,r.plng]);" +
                "var d=L.marker([r.dlat,r.dlng]).addTo(map);" +
                "d.bindPopup('<b>DESTINATION</b><br>'+esc(r.destination)+'<br><b>Status:</b> '+esc(r.status));" +
                "d.on('click',function(){showDetails(r.id)});" +
                "markers.push(d);points.push([r.dlat,r.dlng]);" +
                "});" +
                "if(points.length){map.fitBounds(points,{padding:[25,25],maxZoom:15});}" +
                "else{map.setView([12.8797,121.7740],6);}}" +
                "function showDetails(id){window.location.href='sakayna://booking/'+encodeURIComponent(id);}" +
                "</script></body></html>";

        mapView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                if (activityDestroyed) return;

                mapPageReady = true;
                renderMap();

                view.postDelayed(() -> {
                    if (!activityDestroyed && mapView != null) {
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
                                url.substring("sakayna://booking/".length()),
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
        provinceNames.clear();
        provinceNames.add(ALL_PROVINCES);

        provinceSpinner.setAdapter(adapter(provinceNames));

        List<String> towns = new ArrayList<>();
        towns.add(ALL_TOWNS);
        townSpinner.setAdapter(adapter(towns));

        provinceSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> p,
                            View v,
                            int position,
                            long id
                    ) {
                        if (updatingSpinners || activityDestroyed) return;

                        refreshTownSpinner();
                        renderMap();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> p) {
                    }
                }
        );

        townSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> p,
                            View v,
                            int position,
                            long id
                    ) {
                        if (!updatingSpinners && !activityDestroyed) {
                            renderMap();
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> p) {
                    }
                }
        );
    }

    private ArrayAdapter<String> adapter(List<String> values) {
        ArrayAdapter<String> a = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                values
        );

        a.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        return a;
    }

    // Load provinces and cities/municipalities independently.
    private void loadPhilippineAreas() {
        statusText.setText(
                "Loading Philippine provinces and cities/municipalities..."
        );

        try {
            background.execute(() -> {
                List<String> loadedProvinces = new ArrayList<>();
                loadedProvinces.add(ALL_PROVINCES);

                List<AreaPlace> loadedPlaces = new ArrayList<>();
                Map<String, String> loadedProvinceCodes = new HashMap<>();

                Exception provinceError = null;
                Exception placesError = null;

                try {
                    JSONArray provincesJson = fetchFirstAvailable(
                            "https://psgc.cloud/api/v2/provinces",
                            "https://psgc.cloud/api/v1/provinces",
                            "https://psgc.cloud/api/provinces"
                    );

                    for (int i = 0; i < provincesJson.length(); i++) {
                        JSONObject o = provincesJson.optJSONObject(i);
                        if (o == null) continue;

                        String name = jsonName(o.opt("name"));
                        String code = jsonName(o.opt("code"));

                        if (!name.isEmpty() &&
                                !containsIgnoreCase(loadedProvinces, name)) {
                            loadedProvinces.add(name);
                        }

                        if (!name.isEmpty() && !code.isEmpty()) {
                            loadedProvinceCodes.put(
                                    normalizeCode(code),
                                    name
                            );
                            loadedProvinceCodes.put(
                                    normalizeCode(name),
                                    name
                            );
                        }
                    }

                    if (loadedProvinces.size() <= 1) {
                        throw new Exception(
                                "The province endpoint returned no province names"
                        );
                    }

                } catch (Exception e) {
                    provinceError = e;
                    Log.e(TAG, "Province list request failed", e);
                }

                try {
                    JSONArray placesJson = fetchFirstAvailable(
                            "https://psgc.cloud/api/v2/cities-municipalities",
                            "https://psgc.cloud/api/v1/cities-municipalities",
                            "https://psgc.cloud/api/cities-municipalities"
                    );

                    for (int i = 0; i < placesJson.length(); i++) {
                        JSONObject o = placesJson.optJSONObject(i);
                        if (o == null) continue;

                        String name = jsonName(o.opt("name"));
                        Object provinceValue = o.opt("province");

                        String province = jsonName(provinceValue);
                        String provinceCode = "";

                        if (provinceValue instanceof JSONObject) {
                            JSONObject po = (JSONObject) provinceValue;

                            provinceCode = jsonName(po.opt("code"));

                            if (province.isEmpty()) {
                                province = jsonName(po.opt("name"));
                            }
                        }

                        if (province.isEmpty()) {
                            province = jsonName(o.opt("province_name"));
                        }

                        if (province.isEmpty()) {
                            province = jsonName(o.opt("provinceName"));
                        }

                        if (provinceCode.isEmpty()) {
                            provinceCode = jsonName(o.opt("province_code"));
                        }

                        if (provinceCode.isEmpty()) {
                            provinceCode = jsonName(o.opt("provinceCode"));
                        }

                        String normalizedCode = normalizeCode(provinceCode);

                        if (province.isEmpty() && !normalizedCode.isEmpty()) {
                            province = loadedProvinceCodes.get(normalizedCode);
                        }

                        String normalizedProvince = normalizeCode(province);

                        if (loadedProvinceCodes.containsKey(normalizedProvince)) {
                            province = loadedProvinceCodes.get(normalizedProvince);
                        }

                        String type = jsonName(o.opt("type"));

                        if (name.isEmpty()) continue;

                        loadedPlaces.add(
                                new AreaPlace(
                                        name,
                                        province,
                                        provinceCode,
                                        type
                                )
                        );
                    }

                    if (loadedPlaces.isEmpty()) {
                        throw new Exception(
                                "The city/municipality endpoint returned no places"
                        );
                    }

                } catch (Exception e) {
                    placesError = e;
                    Log.e(TAG, "City/municipality list request failed", e);
                }

                if (loadedProvinces.size() > 1) {
                    Collections.sort(
                            loadedProvinces.subList(
                                    1,
                                    loadedProvinces.size()
                            ),
                            String.CASE_INSENSITIVE_ORDER
                    );
                }

                final List<String> finalProvinces = loadedProvinces;
                final List<AreaPlace> finalPlaces = loadedPlaces;
                final Map<String, String> finalProvinceCodes = loadedProvinceCodes;
                final Exception finalProvinceError = provinceError;
                final Exception finalPlacesError = placesError;

                runOnUiThread(() -> {
                    if (activityDestroyed || isFinishing()) return;

                    provinceNames.clear();

                    if (finalProvinces.size() <= 1) {
                        provinceNames.add(ALL_PROVINCES);
                        provinceNames.addAll(defaultPhilippineProvinces());
                    } else {
                        provinceNames.addAll(finalProvinces);
                    }

                    areaPlaces.clear();
                    areaPlaces.addAll(finalPlaces);

                    provinceNameByCode.clear();
                    provinceNameByCode.putAll(finalProvinceCodes);

                    areaLoadFinished = true;

                    inferRideAreasFromText();

                    for (RideItem ride : rides) {
                        if (ride.province != null &&
                                !ride.province.trim().isEmpty() &&
                                !containsIgnoreCase(
                                        provinceNames,
                                        ride.province.trim()
                                )) {
                            provinceNames.add(ride.province.trim());
                        }
                    }

                    if (provinceNames.size() > 1) {
                        Collections.sort(
                                provinceNames.subList(
                                        1,
                                        provinceNames.size()
                                ),
                                String.CASE_INSENSITIVE_ORDER
                        );
                    }

                    updatingSpinners = true;
                    provinceSpinner.setAdapter(adapter(provinceNames));
                    provinceSpinner.setSelection(0);
                    updatingSpinners = false;

                    refreshTownSpinner();
                    renderMap();

                    if (!areaPlaces.isEmpty()) {
                        statusText.setText(
                                "Philippine area list loaded • " +
                                (provinceNames.size() - 1) +
                                " provinces • " +
                                areaPlaces.size() +
                                " cities/municipalities • Live bookings: " +
                                rides.size()
                        );

                    } else if (finalProvinceError == null &&
                            finalPlacesError != null) {
                        statusText.setText(
                                "Province list loaded; city/municipality list unavailable. " +
                                "Check internet and reopen this screen."
                        );

                    } else {
                        statusText.setText(
                                "Using built-in province list; area data could not load. " +
                                "Check internet and reopen this screen."
                        );
                    }
                });
            });

        } catch (Exception e) {
            Log.e(TAG, "Unable to start area list request", e);

            provinceNames.clear();
            provinceNames.add(ALL_PROVINCES);
            provinceNames.addAll(defaultPhilippineProvinces());

            areaLoadFinished = true;

            updatingSpinners = true;
            provinceSpinner.setAdapter(adapter(provinceNames));
            updatingSpinners = false;

            refreshTownSpinner();
        }
    }

    private JSONArray fetchFirstAvailable(String... addresses)
            throws Exception {

        Exception last = null;

        for (String address : addresses) {
            try {
                JSONArray result = fetchArray(address);

                if (result != null && result.length() > 0) {
                    return result;
                }

                last = new Exception(
                        "No records returned by " + address
                );

            } catch (Exception e) {
                last = e;

                Log.w(
                        TAG,
                        "Area endpoint failed: " + address +
                        " — " + e.getMessage()
                );
            }
        }

        throw last == null
                ? new Exception("No area endpoints configured")
                : last;
    }

    private JSONArray fetchArray(String address) throws Exception {
        HttpURLConnection connection =
                (HttpURLConnection) new URL(address).openConnection();

        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(15000);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "SakayNa-Android/1.0");

        try {
            int code = connection.getResponseCode();

            if (code < 200 || code >= 300) {
                throw new Exception(
                        "Area API returned HTTP " + code + " for " + address
                );
            }

            InputStream in = connection.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            byte[] buffer = new byte[8192];
            int n;

            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }

            in.close();

            String body = out.toString("UTF-8");
            Object parsed =
                    new org.json.JSONTokener(body).nextValue();

            JSONArray array = findArrayInResponse(parsed);

            if (array != null) return array;

            throw new Exception(
                    "Unexpected JSON structure from " + address
            );

        } finally {
            connection.disconnect();
        }
    }

    private JSONArray findArrayInResponse(Object parsed) {
        if (parsed instanceof JSONArray) {
            return (JSONArray) parsed;
        }

        if (!(parsed instanceof JSONObject)) return null;

        JSONObject obj = (JSONObject) parsed;

        String[] arrayKeys = {
                "data",
                "results",
                "items",
                "provinces",
                "cities_municipalities",
                "citiesMunicipalities",
                "cities",
                "municipalities"
        };

        for (String key : arrayKeys) {
            JSONArray array = obj.optJSONArray(key);
            if (array != null) return array;
        }

        for (String key : new String[]{"data", "result", "payload"}) {
            Object nested = obj.opt(key);

            if (nested instanceof JSONObject ||
                    nested instanceof JSONArray) {
                JSONArray result = findArrayInResponse(nested);

                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    private String jsonName(Object value) {
        if (value == null || value == JSONObject.NULL) {
            return "";
        }

        if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;

            String name = jsonName(object.opt("name"));
            if (!name.isEmpty()) return name;

            name = jsonName(object.opt("description"));
            if (!name.isEmpty()) return name;

            name = jsonName(object.opt("code"));
            if (!name.isEmpty()) return name;

            return "";
        }

        String result = String.valueOf(value).trim();

        if ("null".equalsIgnoreCase(result) ||
                "undefined".equalsIgnoreCase(result)) {
            return "";
        }

        return result;
    }

    private String normalizeCode(String value) {
        if (value == null) return "";

        return value
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }

    private boolean containsIgnoreCase(List<String> values, String target) {
        if (values == null || target == null) return false;

        for (String value : values) {
            if (value != null && value.equalsIgnoreCase(target.trim())) {
                return true;
            }
        }

        return false;
    }

    private int findIgnoreCase(List<String> values, String target) {
        if (values == null || target == null) return 0;

        for (int i = 0; i < values.size(); i++) {
            String value = values.get(i);

            if (value != null && value.equalsIgnoreCase(target.trim())) {
                return i;
            }
        }

        return 0;
    }

    private List<String> defaultPhilippineProvinces() {
        List<String> result = new ArrayList<>();

        Collections.addAll(
                result,
                "Abra",
                "Agusan del Norte",
                "Agusan del Sur",
                "Aklan",
                "Albay",
                "Antique",
                "Apayao",
                "Aurora",
                "Basilan",
                "Bataan",
                "Batanes",
                "Batangas",
                "Benguet",
                "Biliran",
                "Bohol",
                "Bukidnon",
                "Bulacan",
                "Cagayan",
                "Camarines Norte",
                "Camarines Sur",
                "Camiguin",
                "Capiz",
                "Catanduanes",
                "Cavite",
                "Cebu",
                "Cotabato",
                "Davao de Oro",
                "Davao del Norte",
                "Davao del Sur",
                "Davao Occidental",
                "Davao Oriental",
                "Dinagat Islands",
                "Eastern Samar",
                "Guimaras",
                "Ifugao",
                "Ilocos Norte",
                "Ilocos Sur",
                "Iloilo",
                "Isabela",
                "Kalinga",
                "La Union",
                "Laguna",
                "Lanao del Norte",
                "Lanao del Sur",
                "Leyte",
                "Maguindanao del Norte",
                "Maguindanao del Sur",
                "Marinduque",
                "Masbate",
                "Misamis Occidental",
                "Misamis Oriental",
                "Mountain Province",
                "Negros Occidental",
                "Negros Oriental",
                "Northern Samar",
                "Nueva Ecija",
                "Nueva Vizcaya",
                "Occidental Mindoro",
                "Oriental Mindoro",
                "Palawan",
                "Pampanga",
                "Pangasinan",
                "Quezon",
                "Quirino",
                "Rizal",
                "Romblon",
                "Samar",
                "Sarangani",
                "Siquijor",
                "Sorsogon",
                "South Cotabato",
                "Southern Leyte",
                "Sultan Kudarat",
                "Sulu",
                "Surigao del Norte",
                "Surigao del Sur",
                "Tarlac",
                "Tawi-Tawi",
                "Zambales",
                "Zamboanga del Norte",
                "Zamboanga del Sur",
                "Zamboanga Sibugay"
        );

        Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
        return result;
    }

    private void inferRideAreasFromText() {
        for (RideItem ride : rides) {
            if (ride == null) continue;

            if (ride.province == null) ride.province = "";
            if (ride.town == null) ride.town = "";

            String combined = (
                    safe(ride.pickup) + " " +
                    safe(ride.destination)
            ).toLowerCase(Locale.ROOT);

            for (AreaPlace place : areaPlaces) {
                if (place == null || place.name == null) continue;

                if (combined.contains(place.name.toLower
