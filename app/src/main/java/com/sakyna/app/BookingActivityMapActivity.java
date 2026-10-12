package com.sakyna.app;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
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

import android.location.Address;
import android.location.Geocoder;

public class BookingActivityMapActivity extends Activity {

    private FirebaseFirestore db;
    private ListenerRegistration ridesListener;

    private LinearLayout root;
    private LinearLayout filters;
    private Spinner provinceSpinner;
    private Spinner townSpinner;
    private WebView mapView;
    private TextView statusText;
    private TextView dateText;
    private TextView detailsText;
    private Button startDateButton;
    private Button endDateButton;

    private final List<RideItem> rides = new ArrayList<>();
    private final Set<String> provinces = new LinkedHashSet<>();
    private final Set<String> towns = new LinkedHashSet<>();

    private final ExecutorService geocoderExecutor =
            Executors.newSingleThreadExecutor();

    private long startOfDay;
    private long endOfDay;

    private long selectedStart;
    private long selectedEnd;

    private boolean updatingSpinners = false;

    private static final String ALL_PROVINCES = "All Provinces";
    private static final String ALL_TOWNS = "All Towns / Cities";

    private static final int GREEN = Color.rgb(0, 125, 75);
    private static final int DARK_GREEN = Color.rgb(0, 95, 55);

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();

        Calendar calendar = Calendar.getInstance();

        calendar.set(
                Calendar.HOUR_OF_DAY, 0
        );
        calendar.set(
                Calendar.MINUTE, 0
        );
        calendar.set(
                Calendar.SECOND, 0
        );
        calendar.set(
                Calendar.MILLISECOND, 0
        );

        startOfDay = calendar.getTimeInMillis();

        calendar.add(Calendar.DAY_OF_MONTH, 1);

        endOfDay = calendar.getTimeInMillis() - 1;

        selectedStart = startOfDay;
        selectedEnd = endOfDay;

        buildScreen();
        setupMap();
        setupSpinners();
        listenForBookings();
    }

    private void buildScreen() {

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(
                Color.rgb(245, 248, 246)
        );

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER);
        header.setPadding(12, 18, 12, 18);
        header.setBackgroundColor(GREEN);

        TextView title = new TextView(this);
        title.setText("SAKAY NA");
        title.setTextColor(Color.WHITE);
        title.setTextSize(25);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, android.graphics.Typeface.BOLD);

        header.addView(
                title,
                new LinearLayout.LayoutParams(
                        -1, -2
                )
        );

        TextView subtitle = new TextView(this);
        subtitle.setText("LIVE BOOKING ACTIVITY MAP");
        subtitle.setTextColor(Color.WHITE);
        subtitle.setTextSize(16);
        subtitle.setGravity(Gravity.CENTER);

        header.addView(
                subtitle,
                new LinearLayout.LayoutParams(
                        -1, -2
                )
        );

        root.addView(
                header,
                new LinearLayout.LayoutParams(
                        -1, -2
                )
        );

        ScrollView scroll = new ScrollView(this);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(12, 12, 12, 12);

        statusText = new TextView(this);
        statusText.setText("Connecting to live bookings...");
        statusText.setTextSize(14);
        statusText.setTextColor(DARK_GREEN);
        statusText.setPadding(4, 8, 4, 8);

        content.addView(statusText);

        TextView provinceLabel = label("PROVINCE");
        content.addView(provinceLabel);

        provinceSpinner = new Spinner(this);
        content.addView(
                provinceSpinner,
                new LinearLayout.LayoutParams(
                        -1, 52
                )
        );

        TextView townLabel = label("TOWN / CITY");
        content.addView(townLabel);

        townSpinner = new Spinner(this);
        content.addView(
                townSpinner,
                new LinearLayout.LayoutParams(
                        -1, 52
                )
        );

        TextView dateLabel = label("BOOKING DATE RANGE");
        content.addView(dateLabel);

        LinearLayout dateRow = new LinearLayout(this);
        dateRow.setOrientation(LinearLayout.HORIZONTAL);

        startDateButton = new Button(this);
        startDateButton.setText("Start Date");
        startDateButton.setOnClickListener(
                v -> showDatePicker(true)
        );

        endDateButton = new Button(this);
        endDateButton.setText("End Date");
        endDateButton.setOnClickListener(
                v -> showDatePicker(false)
        );

        dateRow.addView(
                startDateButton,
                new LinearLayout.LayoutParams(
                        0, -2, 1
                )
        );

        dateRow.addView(
                endDateButton,
                new LinearLayout.LayoutParams(
                        0, -2, 1
                )
        );

        content.addView(dateRow);

        dateText = new TextView(this);
        dateText.setTextSize(13);
        dateText.setTextColor(Color.DKGRAY);
        dateText.setPadding(4, 8, 4, 8);

        content.addView(dateText);

        Button todayButton = new Button(this);
        todayButton.setText("SHOW TODAY'S BOOKINGS");
        todayButton.setTextColor(Color.WHITE);
        todayButton.setBackgroundColor(GREEN);

        todayButton.setOnClickListener(v -> {

            Calendar calendar = Calendar.getInstance();

            calendar.set(
                    Calendar.HOUR_OF_DAY, 0
            );
            calendar.set(
                    Calendar.MINUTE, 0
            );
            calendar.set(
                    Calendar.SECOND, 0
            );
            calendar.set(
                    Calendar.MILLISECOND, 0
            );

            selectedStart = calendar.getTimeInMillis();

            calendar.add(Calendar.DAY_OF_MONTH, 1);

            selectedEnd =
                    calendar.getTimeInMillis() - 1;

            updateDateText();
            renderMap();
        });

        content.addView(
                todayButton,
                new LinearLayout.LayoutParams(
                        -1, -2
                )
        );

        TextView mapLabel = label(
                "PICKUP AND DESTINATION LOCATIONS"
        );

        content.addView(mapLabel);

        mapView = new WebView(this);

        content.addView(
                mapView,
                new LinearLayout.LayoutParams(
                        -1, 520
                )
        );

        TextView detailsLabel = label(
                "SELECTED BOOKING DETAILS"
        );

        content.addView(detailsLabel);

        detailsText = new TextView(this);
        detailsText.setText(
                "Tap a pickup or destination marker on the map."
        );
        detailsText.setTextColor(Color.DKGRAY);
        detailsText.setTextSize(14);
        detailsText.setPadding(12, 12, 12, 12);
        detailsText.setBackgroundColor(Color.WHITE);

        content.addView(
                detailsText,
                new LinearLayout.LayoutParams(
                        -1, -2
                )
        );

        Button backButton = new Button(this);
        backButton.setText("BACK TO ADMIN DASHBOARD");
        backButton.setOnClickListener(
                v -> finish()
        );

        content.addView(
                backButton,
                new LinearLayout.LayoutParams(
                        -1, -2
                )
        );

        scroll.addView(content);

        root.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        -1, 0, 1
                )
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
        view.setPadding(4, 12, 4, 4);

        return view;
    }

    private void setupMap() {

        WebSettings settings =
                mapView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        mapView.setBackgroundColor(
                Color.rgb(235, 241, 237)
        );

        String html =
                "<!DOCTYPE html>" +
                "<html><head>" +
                "<meta name='viewport' " +
                "content='width=device-width, initial-scale=1'>" +
                "<link rel='stylesheet' " +
                "href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>" +
                "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'>" +
                "</script>" +
                "<style>" +
                "html,body,#map{height:100%;width:100%;margin:0;padding:0;}" +
                ".leaflet-popup-content{font-size:14px;}" +
                "</style>" +
                "</head><body>" +
                "<div id='map'></div>" +
                "<script>" +
                "var map=L.map('map').setView([12.8797,121.7740],6);" +
                "L.tileLayer(" +
                "'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'," +
                "{maxZoom:19,attribution:'&copy; OpenStreetMap contributors'}" +
                ").addTo(map);" +
                "var markers=[];" +
                "function clearMarkers(){" +
                "markers.forEach(function(m){map.removeLayer(m);});" +
                "markers=[];" +
                "}" +
                "function addBookingMarkers(data){" +
                "clearMarkers();" +
                "var points=[];" +
                "data.forEach(function(r){" +
                "var pickup=L.marker([r.plat,r.plng]).addTo(map);" +
                "pickup.bindPopup(" +
                "'<b>PICKUP</b><br>'+r.pickup+" +
                "'<br><b>Status:</b> '+r.status+" +
                "'<br><button onclick=\"showDetails(\\''+r.id+'\\')\">Booking details</button>'" +
                ");" +
                "pickup.on('click',function(){" +
                "showDetails(r.id);" +
                "});" +
                "markers.push(pickup);" +
                "points.push([r.plat,r.plng]);" +
                "var dest=L.marker([r.dlat,r.dlng]).addTo(map);" +
                "dest.bindPopup(" +
                "'<b>DESTINATION</b><br>'+r.destination+" +
                "'<br><b>Status:</b> '+r.status+" +
                "'<br><button onclick=\"showDetails(\\''+r.id+'\\')\">Booking details</button>'" +
                ");" +
                "dest.on('click',function(){" +
                "showDetails(r.id);" +
                "});" +
                "markers.push(dest);" +
                "points.push([r.dlat,r.dlng]);" +
                "});" +
                "if(points.length>0){" +
                "map.fitBounds(points,{padding:[25,25],maxZoom:15});" +
                "}" +
                "}" +
                "function showDetails(id){" +
                "window.location.href='sakayna://booking/'+encodeURIComponent(id);" +
                "}" +
                "</script>" +
                "</body></html>";

        mapView.setWebViewClient(
                new android.webkit.WebViewClient() {

                    @Override
                    public boolean shouldOverrideUrlLoading(
                            android.webkit.WebView view,
                            String url
                    ) {

                        if (
                                url != null &&
                                url.startsWith(
                                        "sakayna://booking/"
                                )
                        ) {
                            String id = url.substring(
                                    "sakayna://booking/".length()
                            );

                            try {
                                id = java.net.URLDecoder.decode(
                                        id,
                                        "UTF-8"
                                );

                                showBookingDetails(id);

                            } catch (Exception ignored) {
                            }

                            return true;
                        }

                        return false;
                    }
                }
        );

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
                adapter(Collections.singletonList(
                        ALL_PROVINCES
                ))
        );

        townSpinner.setAdapter(
                adapter(Collections.singletonList(
                        ALL_TOWNS
                ))
        );

        provinceSpinner.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {
                        if (updatingSpinners) return;

                        refreshTownSpinner();
                        renderMap();
                    }

                    @Override
                    public void onNothingSelected(
                            android.widget.AdapterView<?> parent
                    ) {
                    }
                }
        );

        townSpinner.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {
                        if (updatingSpinners) return;

                        renderMap();
                    }

                    @Override
                    public void onNothingSelected(
                            android.widget.AdapterView<?> parent
                    ) {
                    }
                }
        );
    }

    private ArrayAdapter<String> adapter(
            List<String> values
    ) {

        ArrayAdapter<String> result =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        values
                );

        result.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        return result;
    }

    private void listenForBookings() {

        ridesListener =
                db.collection("rides")
                        .addSnapshotListener(
                                (snapshot, error) -> {

                                    if (error != null) {

                                        statusText.setText(
                                                "Unable to load live bookings. " +
                                                "Check connection and Firestore access."
                                        );

                                        return;
                                    }

                                    if (snapshot == null) {
                                        return;
                                    }

                                    rides.clear();

                                    for (
                                            DocumentSnapshot doc :
                                            snapshot.getDocuments()
                                    ) {

                                        RideItem ride =
                                                readRide(doc);

                                        if (ride == null) {
                                            continue;
                                        }

                                        rides.add(ride);
                                    }

                                    statusText.setText(
                                            "Live booking records: " +
                                            rides.size()
                                    );

                                    rebuildLocationFilters();
                                    renderMap();

                                    resolveUnresolvedLocations();
                                }
                        );
    }

    private RideItem readRide(
            DocumentSnapshot doc
    ) {

        Double pickupLat =
                getNumber(doc, "pickupLatitude");

        Double pickupLng =
                getNumber(doc, "pickupLongitude");

        Double destinationLat =
                getNumber(doc, "destinationLatitude");

        Double destinationLng =
                getNumber(doc, "destinationLongitude");

        if (
                pickupLat == null ||
                pickupLng == null ||
                destinationLat == null ||
                destinationLng == null
        ) {
            return null;
        }

        if (
                !validCoordinates(pickupLat, pickupLng) ||
                !validCoordinates(
                        destinationLat,
                        destinationLng
                )
        ) {
            return null;
        }

        RideItem ride =
                new RideItem(doc.getId());

        ride.pickupLat = pickupLat;
        ride.pickupLng = pickupLng;

        ride.destinationLat = destinationLat;
        ride.destinationLng = destinationLng;

        ride.passengerId =
                safe(doc.getString("passengerId"));

        ride.status =
                safe(doc.getString("status"));

        ride.pickup =
                firstNonEmpty(
                        doc.getString("pickupName"),
                        doc.getString("pickup")
                );

        ride.destination =
                firstNonEmpty(
                        doc.getString("destinationName"),
                        doc.getString("destination")
                );

        ride.paymentMethod =
                safe(doc.getString("paymentMethod"));

        ride.paymentStatus =
                safe(doc.getString("paymentStatus"));

        Timestamp created =
                doc.getTimestamp("createdAt");

        if (created != null) {
            ride.createdAt =
                    created.toDate().getTime();
        }

        return ride;
    }

    private Double getNumber(
            DocumentSnapshot doc,
            String key
    ) {

        Object value = doc.get(key);

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        return null;
    }

    private boolean validCoordinates(
            double lat,
            double lng
    ) {

        return lat >= -90 &&
                lat <= 90 &&
                lng >= -180 &&
                lng <= 180 &&
                !(lat == 0 && lng == 0);
    }

    private void resolveUnresolvedLocations() {

        if (!Geocoder.isPresent()) {
            statusText.setText(
                    "Live bookings loaded. " +
                    "Address lookup is unavailable on this device."
            );
            return;
        }

        for (RideItem ride : new ArrayList<>(rides)) {

            if (ride.locationResolved) {
                continue;
            }

            ride.locationResolved = true;

            geocoderExecutor.execute(() -> {

                String province = "";
                String town = "";

                try {

                    Geocoder geocoder =
                            new Geocoder(
                                    getApplicationContext(),
                                    Locale.getDefault()
                            );

                    List<Address> addresses =
                            geocoder.getFromLocation(
                                    ride.pickupLat,
                                    ride.pickupLng,
                                    1
                            );

                    if (
                            addresses != null &&
                            !addresses.isEmpty()
                    ) {

                        Address address =
                                addresses.get(0);

                        province = safe(
                                address.getAdminArea()
                        );

                        town = firstNonEmpty(
                                address.getLocality(),
                                address.getSubAdminArea(),
                                address.getSubLocality()
                        );
                    }

                } catch (Exception ignored) {
                }

                final String resolvedProvince = province;
                final String resolvedTown = town;

                runOnUiThread(() -> {

                    for (RideItem current : rides) {

                        if (current.id.equals(ride.id)) {

                            current.province =
                                    resolvedProvince;

                            current.town =
                                    resolvedTown;

                            break;
                        }
                    }

                    rebuildLocationFilters();
                    renderMap();
                });
            });
        }
    }

    private void rebuildLocationFilters() {

        String previousProvince =
                selectedProvince();

        provinces.clear();
        provinces.add(ALL_PROVINCES);

        for (RideItem ride : rides) {

            if (
                    ride.province != null &&
                    !ride.province.trim().isEmpty()
            ) {
                provinces.add(ride.province);
            }
        }

        List<String> provinceList =
                new ArrayList<>(provinces);

        if (provinceList.size() > 1) {

            List<String> rest =
                    provinceList.subList(
                            1,
                            provinceList.size()
                    );

            Collections.sort(
                    rest,
                    String.CASE_INSENSITIVE_ORDER
            );
        }

        updatingSpinners = true;

        provinceSpinner.setAdapter(
                adapter(provinceList)
        );

        int provinceIndex =
                provinceList.indexOf(previousProvince);

        provinceSpinner.setSelection(
                Math.max(0, provinceIndex)
        );

        updatingSpinners = false;

        refreshTownSpinner();
    }

    private void refreshTownSpinner() {

        String selectedProvince =
                selectedProvince();

        String previousTown =
                selectedTown();

        towns.clear();
        towns.add(ALL_TOWNS);

        for (RideItem ride : rides) {

            if (
                    ALL_PROVINCES.equals(selectedProvince) ||
                    selectedProvince.equals(ride.province)
            ) {

                if (
                        ride.town != null &&
                        !ride.town.trim().isEmpty()
                ) {
                    towns.add(ride.town);
                }
            }
        }

        List<String> townList =
                new ArrayList<>(towns);

        if (townList.size() > 1) {

            List<String> rest =
                    townList.subList(
                            1,
                            townList.size()
                    );

            Collections.sort(
                    rest,
                    String.CASE_INSENSITIVE_ORDER
            );
        }

        updatingSpinners = true;

        townSpinner.setAdapter(
                adapter(townList)
        );

        int index =
                townList.indexOf(previousTown);

        townSpinner.setSelection(
                Math.max(0, index)
        );

        updatingSpinners = false;
    }

    private String selectedProvince() {

        Object selected =
                provinceSpinner.getSelectedItem();

        return selected == null
                ? ALL_PROVINCES
                : selected.toString();
    }

    private String selectedTown() {

        Object selected =
                townSpinner.getSelectedItem();

        return selected == null
                ? ALL_TOWNS
                : selected.toString();
    }

    private void showDatePicker(boolean startDate) {

        Calendar calendar =
                Calendar.getInstance();

        calendar.setTimeInMillis(
                startDate
                        ? selectedStart
                        : selectedEnd
        );

        DatePickerDialog dialog =
                new DatePickerDialog(
                        this,
                        (picker, year, month, day) -> {

                            Calendar chosen =
                                    Calendar.getInstance();

                            chosen.set(
                                    year,
                                    month,
                                    day,
                                    0,
                                    0,
                                    0
                            );

                            chosen.set(
                                    Calendar.MILLISECOND,
                                    0
                            );

                            if (startDate) {

                                selectedStart =
                                        chosen.getTimeInMillis();

                            } else {

                                chosen.add(
                                        Calendar.DAY_OF_MONTH,
                                        1
                                );

                                selectedEnd =
                                        chosen.getTimeInMillis() - 1;
                            }

                            if (selectedEnd < selectedStart) {

                                Toast.makeText(
                                        this,
                                        "End date must not be before start date.",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            updateDateText();
                            renderMap();
                        },
                        calendar.get(
                                Calendar.YEAR
                        ),
                        calendar.get(
                                Calendar.MONTH
                        ),
                        calendar.get(
                                Calendar.DAY_OF_MONTH
                        )
                );

        dialog.show();
    }

    private void updateDateText() {

        SimpleDateFormat format =
                new SimpleDateFormat(
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

        if (mapView == null) {
            return;
        }

        String province = selectedProvince();
        String town = selectedTown();

        JSONArray array = new JSONArray();

        int visible = 0;

        for (RideItem ride : rides) {

            if (
                    ride.createdAt < selectedStart ||
                    ride.createdAt > selectedEnd
            ) {
                continue;
            }

            if (
                    !ALL_PROVINCES.equals(province) &&
                    !province.equals(ride.province)
            ) {
                continue;
            }

            if (
                    !ALL_TOWNS.equals(town) &&
                    !town.equals(ride.town)
            ) {
                continue;
            }

            try {

                JSONObject item =
                        new JSONObject();

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

            } catch (Exception ignored) {
            }
        }

        statusText.setText(
                "Live booking records: " +
                rides.size() +
                " • Matching bookings: " +
                visible
        );

        String json = array.toString();

        String script =
                "javascript:addBookingMarkers(" +
                json +
                ")";

        mapView.post(
                () -> mapView.evaluateJavascript(
                        script,
                        null
                )
        );
    }

    private void showBookingDetails(String id) {

        for (RideItem ride : rides) {

            if (!ride.id.equals(id)) {
                continue;
            }

            SimpleDateFormat format =
                    new SimpleDateFormat(
                            "dd MMM yyyy, hh:mm a",
                            Locale.getDefault()
                    );

            String text =
                    "BOOKING ID\n" +
                    ride.id +
                    "\n\nSTATUS\n" +
                    ride.status +
                    "\n\nPICKUP\n" +
                    ride.pickup +
                    "\n\nDESTINATION\n" +
                    ride.destination +
                    "\n\nPASSENGER ID\n" +
                    ride.passengerId +
                    "\n\nPAYMENT METHOD\n" +
                    ride.paymentMethod +
                    "\n\nPAYMENT STATUS\n" +
                    ride.paymentStatus +
                    "\n\nBOOKING DATE\n" +
                    (
                            ride.createdAt > 0
                                    ? format.format(
                                            new Date(
                                                    ride.createdAt
                                            )
                                    )
                                    : "Not available"
                    );

            detailsText.setText(text);

            return;
        }

        detailsText.setText(
                "Booking details are no longer available."
        );
    }

    private String firstNonEmpty(
            String first,
            String second
    ) {

        if (
                first != null &&
                !first.trim().isEmpty()
        ) {
            return first;
        }

        if (
                second != null &&
                !second.trim().isEmpty()
        ) {
            return second;
        }

        return "Not available";
    }

    private String safe(String value) {

        return value == null
                ? ""
                : value.trim();
    }

    @Override
    protected void onDestroy() {

        if (ridesListener != null) {
            ridesListener.remove();
            ridesListener = null;
        }

        geocoderExecutor.shutdownNow();

        if (mapView != null) {
            mapView.stopLoading();
            mapView.destroy();
            mapView = null;
        }

        super.onDestroy();
    }
}
