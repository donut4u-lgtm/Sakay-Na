
package com.sakyna.app;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapActivity extends Activity {

    private static final int LOCATION_PERMISSION_REQUEST = 9001;

    private WebView webView;
    private TextView statusText;
    private TextView routeText;

    private FirebaseFirestore db;

    private ListenerRegistration rideListener;
    private ListenerRegistration driverLocationListener;

    private final Handler handler = new Handler();

    private final ExecutorService routeExecutor =
            Executors.newSingleThreadExecutor();

    private String rideId = "";

    private double passengerLatitude = 0.0;
    private double passengerLongitude = 0.0;

    private double destinationLatitude = 0.0;
    private double destinationLongitude = 0.0;

    private double driverLatitude = 0.0;
    private double driverLongitude = 0.0;

    private boolean hasPassengerLocation = false;
    private boolean hasDestinationLocation = false;
    private boolean hasDriverLocation = false;

    private boolean mapReady = false;

    private String currentRideStatus = "";

    private long lastRouteRequestTime = 0L;

    private double lastRoutedDriverLatitude = 0.0;
    private double lastRoutedDriverLongitude = 0.0;

    private final Runnable refreshRunnable =
            new Runnable() {
                @Override
                public void run() {

                    refreshDriverRoute();

                    handler.postDelayed(
                            this,
                            10000
                    );
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();

        readIntentData();

        buildScreen();

        setupWebView();

        requestLocationPermissionIfNeeded();

        if (!rideId.isEmpty()) {
            listenToRide();
            listenToDriverLocation();
        }

        handler.postDelayed(
                refreshRunnable,
                3000
        );
    }

    private void readIntentData() {

        if (getIntent() == null) {
            return;
        }

        rideId =
                safeString(
                        getIntent().getStringExtra("ride_id")
                );

        passengerLatitude =
                getIntent().getDoubleExtra(
                        "passenger_latitude",
                        getIntent().getDoubleExtra(
                                "passengerLatitude",
                                0.0
                        )
                );

        passengerLongitude =
                getIntent().getDoubleExtra(
                        "passenger_longitude",
                        getIntent().getDoubleExtra(
                                "passengerLongitude",
                                0.0
                        )
                );

        destinationLatitude =
                getIntent().getDoubleExtra(
                        "destination_latitude",
                        getIntent().getDoubleExtra(
                                "destinationLatitude",
                                0.0
                        )
                );

        destinationLongitude =
                getIntent().getDoubleExtra(
                        "destination_longitude",
                        getIntent().getDoubleExtra(
                                "destinationLongitude",
                                0.0
                        )
                );

        driverLatitude =
                getIntent().getDoubleExtra(
                        "driver_latitude",
                        getIntent().getDoubleExtra(
                                "driverLatitude",
                                0.0
                        )
                );

        driverLongitude =
                getIntent().getDoubleExtra(
                        "driver_longitude",
                        getIntent().getDoubleExtra(
                                "driverLongitude",
                                0.0
                        )
                );

        hasPassengerLocation =
                isValidCoordinate(
                        passengerLatitude,
                        passengerLongitude
                );

        hasDestinationLocation =
                isValidCoordinate(
                        destinationLatitude,
                        destinationLongitude
                );

        hasDriverLocation =
                isValidCoordinate(
                        driverLatitude,
                        driverLongitude
                );
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
                "🛺 SAKAY NA • LIVE RIDE MAP"
        );

        title.setTextSize(20);

        title.setTextColor(
                Color.BLACK
        );

        title.setGravity(
                android.view.Gravity.CENTER
        );

        title.setPadding(
                10,
                18,
                10,
                10
        );

        root.addView(
                title,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        statusText =
                new TextView(this);

        statusText.setText(
                "Connecting to ride..."
        );

        statusText.setTextSize(15);

        statusText.setTextColor(
                Color.DKGRAY
        );

        statusText.setGravity(
                android.view.Gravity.CENTER
        );

        statusText.setPadding(
                10,
                5,
                10,
                5
        );

        root.addView(
                statusText,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        routeText =
                new TextView(this);

        routeText.setText(
                "Preparing route..."
        );

        routeText.setTextSize(14);

        routeText.setTextColor(
                Color.DKGRAY
        );

        routeText.setGravity(
                android.view.Gravity.CENTER
        );

        routeText.setPadding(
                10,
                5,
                10,
                8
        );

        root.addView(
                routeText,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        webView =
                new WebView(this);

        LinearLayout.LayoutParams mapParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0
                );

        mapParams.weight = 1;

        root.addView(
                webView,
                mapParams
        );

        setContentView(root);
    }

    private void setupWebView() {

        WebSettings settings =
                webView.getSettings();

        settings.setJavaScriptEnabled(true);

        settings.setDomStorageEnabled(true);

        settings.setAllowFileAccess(true);

        settings.setAllowContentAccess(true);

        webView.setWebViewClient(
                new WebViewClient()
        );

        webView.setBackgroundColor(
                Color.WHITE
        );

        String html =
                createMapHtml();

        webView.loadDataWithBaseURL(
                "https://localhost/",
                html,
                "text/html",
                "UTF-8",
                null
        );
    }

    private String createMapHtml() {

        String passengerLat =
                String.format(
                        Locale.US,
                        "%.7f",
                        hasPassengerLocation
                                ? passengerLatitude
                                : 14.000000
                );

        String passengerLon =
                String.format(
                        Locale.US,
                        "%.7f",
                        hasPassengerLocation
                                ? passengerLongitude
                                : 121.000000
                );

        return
                "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +

                "<meta name='viewport' " +
                "content='width=device-width, " +
                "initial-scale=1.0, " +
                "maximum-scale=1.0, " +
                "user-scalable=no'>" +

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

                "var map = L.map('map').setView([" +
                passengerLat +
                "," +
                passengerLon +
                "],15);" +

                "L.tileLayer(" +
                "'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'," +
                "{maxZoom:19," +
                "attribution:'© OpenStreetMap contributors'}" +
                ").addTo(map);" +

                "var passengerMarker=null;" +
                "var driverMarker=null;" +
                "var destinationMarker=null;" +
                "var driverRoute=null;" +
                "var passengerRoute=null;" +

                "function passenger(lat,lon){" +

                "if(passengerMarker){" +
                "passengerMarker.setLatLng([lat,lon]);" +
                "}else{" +
                "passengerMarker=L.marker([lat,lon])" +
                ".addTo(map)" +
                ".bindPopup('📍 Passenger');" +
                "}" +

                "}" +

                "function driver(lat,lon){" +

                "if(driverMarker){" +
                "driverMarker.setLatLng([lat,lon]);" +
                "}else{" +
                "driverMarker=L.marker([lat,lon])" +
                ".addTo(map)" +
                ".bindPopup('🛺 Driver');" +
                "}" +

                "}" +

                "function destination(lat,lon){" +

                "if(destinationMarker){" +
                "destinationMarker.setLatLng([lat,lon]);" +
                "}else{" +
                "destinationMarker=L.marker([lat,lon])" +
                ".addTo(map)" +
                ".bindPopup('🏁 Destination');" +
                "}" +

                "}" +

                "function passengerRoute(coords){" +

                "if(passengerRoute){" +
                "map.removeLayer(passengerRoute);" +
                "}" +

                "passengerRoute=L.polyline(" +
                "coords," +
                "{color:'#1565c0'," +
                "weight:5," +
                "opacity:0.8}" +
                ").addTo(map);" +

                "}" +

                "function driverRoute(coords){" +

                "if(driverRoute){" +
                "map.removeLayer(driverRoute);" +
                "}" +

                "driverRoute=L.polyline(" +
                "coords," +
                "{color:'#e65100'," +
                "weight:5," +
                "opacity:0.9}" +
                ").addTo(map);" +

                "}" +

                "function clearRoutes(){" +

                "if(driverRoute){" +
                "map.removeLayer(driverRoute);" +
                "driverRoute=null;" +
                "}" +

                "if(passengerRoute){" +
                "map.removeLayer(passengerRoute);" +
                "passengerRoute=null;" +
                "}" +

                "}" +

                "</script>" +

                "</body>" +

                "</html>";
    }

    private void listenToRide() {

        rideListener =
                db.collection("rides")
                        .document(rideId)
                        .addSnapshotListener(
                                (snapshot, error) -> {

                                    if (error != null) {

                                        setRoutingStatus(
                                                "Ride listener error"
                                        );

                                        return;
                                    }

                                    if (snapshot == null ||
                                            !snapshot.exists()) {

                                        setRoutingStatus(
                                                "Ride not found"
                                        );

                                        return;
                                    }

                                    updateRideFromFirestore(
                                            snapshot
                                    );
                                }
                        );
    }

    private void updateRideFromFirestore(
            DocumentSnapshot document) {

        String status =
                document.getString(
                        "status"
                );

        if (status != null) {

            currentRideStatus =
                    status;

            setRoutingStatus(
                    "Ride status: " +
                    formatStatus(status)
            );
        }

        Double pLat =
                document.getDouble(
                        "pickupLatitude"
                );

        Double pLon =
                document.getDouble(
                        "pickupLongitude"
                );

        Double dLat =
                document.getDouble(
                        "destinationLatitude"
                );

        Double dLon =
                document.getDouble(
                        "destinationLongitude"
                );

        if (pLat != null && pLon != null) {

            passengerLatitude =
                    pLat;

            passengerLongitude =
                    pLon;

            hasPassengerLocation =
                    isValidCoordinate(
                            pLat,
                            pLon
                    );

            updatePassengerMarker();
        }

        if (dLat != null && dLon != null) {

            destinationLatitude =
                    dLat;

            destinationLongitude =
                    dLon;

            hasDestinationLocation =
                    isValidCoordinate(
                            dLat,
                            dLon
                    );

            updateDestinationMarker();
        }

        refreshDriverRoute();
        requestPassengerRoute();
    }

    private void listenToDriverLocation() {

        if (rideId.isEmpty()) {
            return;
        }

        db.collection("rides")
                .document(rideId)
                .addSnapshotListener(
                        (snapshot, error) -> {

                            if (error != null ||
                                    snapshot == null ||
                                    !snapshot.exists()) {

                                return;
                            }

                            String driverId =
                                    snapshot.getString(
                                            "driverId"
                                    );

                            if (driverId == null ||
                                    driverId.trim().isEmpty()) {

                                return;
                            }

                            listenToDriverDocument(
                                    driverId
                            );
                        }
                );
    }

    private void listenToDriverDocument(
            String driverId) {

        if (driverLocationListener != null) {

            driverLocationListener.remove();

            driverLocationListener =
                    null;
        }

        driverLocationListener =
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
                                                    "latitude"
                                            );

                                    Double lon =
                                            snapshot.getDouble(
                                                    "longitude"
                                            );

                                    if (lat == null ||
                                            lon == null) {

                                        return;
                                    }

                                    driverLatitude =
                                            lat;

                                    driverLongitude =
                                            lon;

                                    hasDriverLocation =
                                            isValidCoordinate(
                                                    lat,
                                                    lon
                                            );

                                    updateDriverMarker();

                                    refreshDriverRoute();
                                }
                        );
    }

    private void updatePassengerMarker() {

        if (!mapReady ||
                !hasPassengerLocation ||
                webView == null) {

            return;
        }

        final double lat =
                passengerLatitude;

        final double lon =
                passengerLongitude;

        runOnUiThread(
                () -> {

                    webView.evaluateJavascript(
                            "passenger(" +
                            lat +
                            "," +
                            lon +
                            ");",
                            null
                    );
                }
        );
    }

    private void updateDriverMarker() {

        if (!mapReady ||
                !hasDriverLocation ||
                webView == null) {

            return;
        }

        final double lat =
                driverLatitude;

        final double lon =
                driverLongitude;

        runOnUiThread(
                () -> {

                    webView.evaluateJavascript(
                            "driver(" +
                            lat +
                            "," +
                            lon +
                            ");",
                            null
                    );
                }
        );
    }

    private void updateDestinationMarker() {

        if (!mapReady ||
                !hasDestinationLocation ||
                webView == null) {

            return;
        }

        final double lat =
                destinationLatitude;

        final double lon =
                destinationLongitude;

        runOnUiThread(
                () -> {

                    webView.evaluateJavascript(
                            "destination(" +
                            lat +
                            "," +
                            lon +
                            ");",
                            null
                    );
                }
        );
    }

    private void refreshDriverRoute() {

        if (!hasDriverLocation ||
                !hasPassengerLocation) {

            return;
        }

        long now =
                System.currentTimeMillis();

        if (now - lastRouteRequestTime < 3000) {

            return;
        }

        if (lastRoutedDriverLatitude != 0.0 &&
                lastRoutedDriverLongitude != 0.0) {

            float[] distance =
                    new float[1];

            Location.distanceBetween(
                    lastRoutedDriverLatitude,
                    lastRoutedDriverLongitude,
                    driverLatitude,
                    driverLongitude,
                    distance
            );

            if (distance[0] < 50) {
                return;
            }
        }

        lastRouteRequestTime =
                now;

        final double fromLat =
                driverLatitude;

        final double fromLon =
                driverLongitude;

        final double toLat =
                passengerLatitude;

        final double toLon =
                passengerLongitude;

        lastRoutedDriverLatitude =
                fromLat;

        lastRoutedDriverLongitude =
                fromLon;

        routeExecutor.execute(
                () -> {

                    RouteResult result =
                            getRoadRoute(
                                    fromLat,
                                    fromLon,
                                    toLat,
                                    toLon
                            );

                    if (result == null) {

                        return;
                    }

                    runOnUiThread(
                            () -> {

                                drawDriverRoute(
                                        result
                                );
                            }
                    );
                }
        );
    }

    private void requestPassengerRoute() {

        if (!hasPassengerLocation ||
                !hasDestinationLocation) {

            return;
        }

        final double fromLat =
                passengerLatitude;

        final double fromLon =
                passengerLongitude;

        final double toLat =
                destinationLatitude;

        final double toLon =
                destinationLongitude;

        routeExecutor.execute(
                () -> {

                    RouteResult result =
                            getRoadRoute(
                                    fromLat,
                                    fromLon,
                                    toLat,
                                    toLon
                            );

                    if (result == null) {

                        return;
                    }

                    runOnUiThread(
                            () -> {

                                drawPassengerRoute(
                                        result
                                );
                            }
                    );
                }
        );
    }

    private RouteResult getRoadRoute(
            double fromLat,
            double fromLon,
            double toLat,
            double toLon) {

        HttpURLConnection connection =
                null;

        try {

            String urlString =
                    "https://router.project-osrm.org/route/v1/driving/" +
                    fromLon +
                    "," +
                    fromLat +
                    ";" +
                    toLon +
                    "," +
                    toLat +
                    "?overview=full&geometries=geojson";

            URL url =
                    new URL(
                            urlString
                    );

            connection =
                    (HttpURLConnection)
                            url.openConnection();

            connection.setRequestMethod(
                    "GET"
            );

            connection.setConnectTimeout(
                    15000
            );

            connection.setReadTimeout(
                    20000
            );

            connection.setRequestProperty(
                    "User-Agent",
                    "SakayNaAndroidApp/1.0"
            );

            int responseCode =
                    connection.getResponseCode();

            if (responseCode !=
                    HttpURLConnection.HTTP_OK) {

                return null;
            }

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

            JSONObject root =
                    new JSONObject(
                            response.toString()
                    );

            String code =
                    root.optString(
                            "code",
                            ""
                    );

            if (!"Ok".equalsIgnoreCase(code)) {

                return null;
            }

            JSONArray routes =
                    root.optJSONArray(
                            "routes"
                    );

            if (routes == null ||
                    routes.length() == 0) {

                return null;
            }

            JSONObject route =
                    routes.getJSONObject(0);

            double distanceMeters =
                    route.optDouble(
                            "distance",
                            0.0
                    );

            double durationSeconds =
                    route.optDouble(
                            "duration",
                            0.0
                    );

            JSONObject geometry =
                    route.optJSONObject(
                            "geometry"
                    );

            if (geometry == null) {

                return null;
            }

            JSONArray coordinates =
                    geometry.optJSONArray(
                            "coordinates"
                    );

            if (coordinates == null ||
                    coordinates.length() == 0) {

                return null;
            }

            StringBuilder jsCoordinates =
                    new StringBuilder();

            jsCoordinates.append(
                    "["
            );

            for (int i = 0;
                    i < coordinates.length();
                    i++) {

                JSONArray point =
                        coordinates.getJSONArray(i);

                double lon =
                        point.getDouble(0);

                double lat =
                        point.getDouble(1);

                if (i > 0) {
                    jsCoordinates.append(",");
                }

                jsCoordinates
                        .append("[")
                        .append(lat)
                        .append(",")
                        .append(lon)
                        .append("]");
            }

            jsCoordinates.append(
                    "]"
            );

            return new RouteResult(
                    distanceMeters,
                    durationSeconds,
                    jsCoordinates.toString()
            );

        } catch (Exception e) {

            return null;

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void drawDriverRoute(
            RouteResult result) {

        if (webView == null) {
            return;
        }

        webView.evaluateJavascript(
                "driverRoute(" +
                result.coordinatesJson +
                ");",
                null
        );

        String distanceText =
                formatDistance(
                        result.distanceMeters
                );

        String durationText =
                formatDuration(
                        result.durationSeconds
                );

        setRouteText(
                "🛺 Driver → Passenger: " +
                distanceText +
                " • " +
                durationText
        );
    }

    private void drawPassengerRoute(
            RouteResult result) {

        if (webView == null) {
            return;
        }

        webView.evaluateJavascript(
                "passengerRoute(" +
                result.coordinatesJson +
                ");",
                null
        );
    }

    /*
     * IMPORTANT:
     * This method was missing in the previous MapActivity.
     * The compiler error was:
     *
     * cannot find symbol
     * method setRoutingStatus(String)
     *
     * It is now included here.
     */
    private void setRoutingStatus(
            String message) {

        runOnUiThread(
                () -> {

                    if (statusText != null) {

                        statusText.setText(
                                message
                        );
                    }
                }
        );
    }

    private void setRouteText(
            String message) {

        runOnUiThread(
                () -> {

                    if (routeText != null) {

                        routeText.setText(
                                message
                        );
                    }
                }
        );
    }

    private String formatStatus(
            String status) {

        if (status == null ||
                status.trim().isEmpty()) {

            return "UNKNOWN";
        }

        return status
                .replace(
                        "_",
                        " "
                )
                .toUpperCase(
                        Locale.US
                );
    }

    private String formatDistance(
            double meters) {

        if (meters < 1000) {

            return String.format(
                    Locale.US,
                    "%.0f m",
                    meters
            );
        }

        return String.format(
                Locale.US,
                "%.2f km",
                meters / 1000.0
        );
    }

    private String formatDuration(
            double seconds) {

        int minutes =
                (int) Math.round(
                        seconds / 60.0
                );

        if (minutes < 1) {
            return "< 1 min";
        }

        if (minutes == 1) {
            return "1 min";
        }

        return minutes +
                " mins";
    }

    private boolean isValidCoordinate(
            double latitude,
            double longitude) {

        return latitude >= -90.0 &&
                latitude <= 90.0 &&
                longitude >= -180.0 &&
                longitude <= 180.0 &&
                !(latitude == 0.0 &&
                        longitude == 0.0);
    }

    private String safeString(
            String value) {

        if (value == null) {
            return "";
        }

        return value;
    }

    private void requestLocationPermissionIfNeeded() {

        if (android.os.Build.VERSION.SDK_INT >= 23) {

            if (checkSelfPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        },
                        LOCATION_PERMISSION_REQUEST
                );
            }
        }
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
                LOCATION_PERMISSION_REQUEST) {

            if (grantResults.length > 0 &&
                    grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED) {

                setRoutingStatus(
                        "🟢 Location permission enabled"
                );
            }
        }
    }

    @Override
    protected void onDestroy() {

        handler.removeCallbacks(
                refreshRunnable
        );

        if (rideListener != null) {

            rideListener.remove();

            rideListener =
                    null;
        }

        if (driverLocationListener != null) {

            driverLocationListener.remove();

            driverLocationListener =
                    null;
        }

        routeExecutor.shutdownNow();

        if (webView != null) {

            webView.stopLoading();

            webView.destroy();

            webView =
                    null;
        }

        super.onDestroy();
    }

    private static class RouteResult {

        final double distanceMeters;

        final double durationSeconds;

        final String coordinatesJson;

        RouteResult(
                double distanceMeters,
                double durationSeconds,
                String coordinatesJson) {

            this.distanceMeters =
                    distanceMeters;

            this.durationSeconds =
                    durationSeconds;

            this.coordinatesJson =
                    coordinatesJson;
        }
    }
}
