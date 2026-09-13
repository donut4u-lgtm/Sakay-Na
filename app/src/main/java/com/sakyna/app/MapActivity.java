
package com.sakyna.app;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

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
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapActivity extends Activity {

    private WebView webView;

    private FirebaseFirestore db;

    private ListenerRegistration driverLocationListener;
    private ListenerRegistration rideListener;

    private final ExecutorService routeExecutor =
            Executors.newSingleThreadExecutor();

    private String rideId = "";
    private String driverId = "";

    private double passengerLatitude = 14.2456;
    private double passengerLongitude = 121.4451;

    private double destinationLatitude = 14.2456;
    private double destinationLongitude = 121.4451;

    private double driverLatitude = 0.0;
    private double driverLongitude = 0.0;

    private boolean pageReady = false;

    private double lastRouteDriverLatitude = 0.0;
    private double lastRouteDriverLongitude = 0.0;

    private long lastRouteRequestTime = 0L;

    private static final long ROUTE_UPDATE_INTERVAL =
            10000L;

    private static final double ROUTE_UPDATE_DISTANCE_KM =
            0.05;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();

        webView = new WebView(this);

        WebSettings settings =
                webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        webView.setWebViewClient(
                new WebViewClient()
        );

        setContentView(webView);

        readIntentData();

        loadMap();

        listenToRide();
    }

    private void readIntentData() {

        if (getIntent().hasExtra("ride_id")) {
            rideId =
                    getIntent().getStringExtra(
                            "ride_id"
                    );
        }

        if (getIntent().hasExtra("driver_id")) {
            driverId =
                    getIntent().getStringExtra(
                            "driver_id"
                    );
        }

        if (getIntent().hasExtra(
                "passenger_latitude")) {

            passengerLatitude =
                    getIntent().getDoubleExtra(
                            "passenger_latitude",
                            passengerLatitude
                    );
        }

        if (getIntent().hasExtra(
                "passenger_longitude")) {

            passengerLongitude =
                    getIntent().getDoubleExtra(
                            "passenger_longitude",
                            passengerLongitude
                    );
        }

        if (getIntent().hasExtra(
                "destination_latitude")) {

            destinationLatitude =
                    getIntent().getDoubleExtra(
                            "destination_latitude",
                            destinationLatitude
                    );
        }

        if (getIntent().hasExtra(
                "destination_longitude")) {

            destinationLongitude =
                    getIntent().getDoubleExtra(
                            "destination_longitude",
                            destinationLongitude
                    );
        }

        if (getIntent().hasExtra(
                "driver_latitude")) {

            driverLatitude =
                    getIntent().getDoubleExtra(
                            "driver_latitude",
                            0.0
                    );
        }

        if (getIntent().hasExtra(
                "driver_longitude")) {

            driverLongitude =
                    getIntent().getDoubleExtra(
                            "driver_longitude",
                            0.0
                    );
        }
    }

    private void loadMap() {

        String html =
                "<!DOCTYPE html>" +

                "<html>" +

                "<head>" +

                "<meta name='viewport' " +
                "content='width=device-width, initial-scale=1.0'>" +

                "<style>" +

                "html,body,#map{" +
                "height:100%;" +
                "margin:0;" +
                "padding:0;" +
                "}" +

                ".infoBox{" +
                "position:absolute;" +
                "top:10px;" +
                "left:10px;" +
                "right:10px;" +
                "z-index:9999;" +
                "background:white;" +
                "padding:14px;" +
                "border-radius:14px;" +
                "box-shadow:0 2px 10px rgba(0,0,0,.3);" +
                "font-family:Arial,sans-serif;" +
                "font-size:14px;" +
                "line-height:1.5;" +
                "}" +

                ".title{" +
                "font-size:19px;" +
                "font-weight:bold;" +
                "margin-bottom:5px;" +
                "}" +

                ".routeInfo{" +
                "margin-top:5px;" +
                "}" +

                "</style>" +

                "<link rel='stylesheet' " +
                "href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'>" +

                "</head>" +

                "<body>" +

                "<div class='infoBox'>" +

                "<div class='title'>🛺 SAKAY NA</div>" +

                "<div id='status'>" +
                "Preparing road route..." +
                "</div>" +

                "<div id='driverRoute' " +
                "class='routeInfo'></div>" +

                "<div id='passengerRoute' " +
                "class='routeInfo'></div>" +

                "<div id='totalRoute' " +
                "class='routeInfo'></div>" +

                "</div>" +

                "<div id='map'></div>" +

                "<script src=" +
                "'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'>" +
                "</script>" +

                "<script>" +

                "var map = L.map('map')" +
                ".setView([14.2456,121.4451],13);" +

                "L.tileLayer(" +
                "'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'," +
                "{" +
                "maxZoom:19," +
                "attribution:" +
                "'© OpenStreetMap contributors'" +
                "}" +
                ").addTo(map);" +

                "var passengerMarker=null;" +
                "var driverMarker=null;" +
                "var destinationMarker=null;" +

                "var driverRouteLine=null;" +
                "var passengerRouteLine=null;" +

                "var passengerIcon=L.divIcon({" +
                "className:''," +
                "html:'<div style=\"font-size:32px\">👤</div>'," +
                "iconSize:[40,40]," +
                "iconAnchor:[20,20]" +
                "});" +

                "var driverIcon=L.divIcon({" +
                "className:''," +
                "html:'<div style=\"font-size:34px\">🛺</div>'," +
                "iconSize:[40,40]," +
                "iconAnchor:[20,20]" +
                "});" +

                "var destinationIcon=L.divIcon({" +
                "className:''," +
                "html:'<div style=\"font-size:32px\">📍</div>'," +
                "iconSize:[40,40]," +
                "iconAnchor:[20,40]" +
                "});" +

                "function setPassenger(lat,lon){" +

                "if(passengerMarker==null){" +

                "passengerMarker=L.marker([lat,lon],{" +
                "icon:passengerIcon" +
                "}).addTo(map)" +
                ".bindPopup(" +
                "'<b>Passenger</b><br>Pickup location'" +
                ");" +

                "}else{" +

                "passengerMarker.setLatLng([lat,lon]);" +

                "}" +

                "}" +

                "function setDestination(lat,lon){" +

                "if(destinationMarker==null){" +

                "destinationMarker=L.marker([lat,lon],{" +
                "icon:destinationIcon" +
                "}).addTo(map)" +
                ".bindPopup(" +
                "'<b>Destination</b>'" +
                ");" +

                "}else{" +

                "destinationMarker.setLatLng([lat,lon]);" +

                "}" +

                "}" +

                "function setDriver(lat,lon){" +

                "if(driverMarker==null){" +

                "driverMarker=L.marker([lat,lon],{" +
                "icon:driverIcon" +
                "}).addTo(map)" +
                ".bindPopup(" +
                "'<b>Sakay Na Driver</b><br>Live location'" +
                ");" +

                "}else{" +

                "driverMarker.setLatLng([lat,lon]);" +

                "}" +

                "document.getElementById('status')" +
                ".innerHTML='🟢 Driver is live';" +

                "}" +

                "function drawDriverRoute(points){" +

                "if(driverRouteLine!=null){" +
                "map.removeLayer(driverRouteLine);" +
                "}" +

                "if(points.length>1){" +

                "driverRouteLine=" +
                "L.polyline(points,{weight:6,opacity:0.85})" +
                ".addTo(map);" +

                "}" +

                "}" +

                "function drawPassengerRoute(points){" +

                "if(passengerRouteLine!=null){" +
                "map.removeLayer(passengerRouteLine);" +
                "}" +

                "if(points.length>1){" +

                "passengerRouteLine=" +
                "L.polyline(points,{weight:6,opacity:0.85})" +
                ".addTo(map);" +

                "}" +

                "}" +

                "function updateDriverRouteInfo(" +
                "distance,duration){" +

                "document.getElementById(" +
                "'driverRoute'" +
                ").innerHTML=" +

                "'🛺 Driver → Passenger: ' +" +
                "distance.toFixed(2)+" +
                "' km • '+duration.toFixed(0)+" +
                "' min';" +

                "}" +

                "function updatePassengerRouteInfo(" +
                "distance,duration){" +

                "document.getElementById(" +
                "'passengerRoute'" +
                ").innerHTML=" +

                "'👤 Passenger → Destination: ' +" +
                "distance.toFixed(2)+" +
                "' km • '+duration.toFixed(0)+" +
                "' min';" +

                "}" +

                "function updateTotalInfo(" +
                "distance,duration){" +

                "document.getElementById(" +
                "'totalRoute'" +
                ").innerHTML=" +

                "'📏 Total road route: ' +" +
                "distance.toFixed(2)+" +
                "' km • '+duration.toFixed(0)+" +
                "' min';" +

                "}" +

                "function clearDriverRoute(){" +

                "if(driverRouteLine!=null){" +
                "map.removeLayer(driverRouteLine);" +
                "driverRouteLine=null;" +
                "}" +

                "document.getElementById(" +
                "'driverRoute'" +
                ").innerHTML='';" +

                "}" +

                "function setRoutingStatus(text){" +

                "document.getElementById(" +
                "'status'" +
                ").innerHTML=text;" +

                "}" +

                "function fitAll(){" +

                "var points=[];" +

                "if(passengerMarker!=null)" +
                "points.push(passengerMarker.getLatLng());" +

                "if(driverMarker!=null)" +
                "points.push(driverMarker.getLatLng());" +

                "if(destinationMarker!=null)" +
                "points.push(destinationMarker.getLatLng());" +

                "if(points.length>0){" +

                "map.fitBounds(points,{padding:[60,60]});" +

                "}" +

                "}" +

                "</script>" +

                "</body>" +

                "</html>";

        webView.loadDataWithBaseURL(
                "https://www.openstreetmap.org/",
                html,
                "text/html",
                "UTF-8",
                null
        );

        webView.postDelayed(() -> {

            pageReady = true;

            updatePassengerOnMap();
            updateDestinationOnMap();

            if (driverLatitude != 0.0 &&
                    driverLongitude != 0.0) {

                updateDriverOnMap(
                        driverLatitude,
                        driverLongitude
                );
            }

            requestPassengerDestinationRoute();

            if (driverLatitude != 0.0 &&
                    driverLongitude != 0.0) {

                requestDriverPassengerRoute();
            }

            fitMap();

        }, 1500);
    }

    private void listenToRide() {

        if (rideId == null ||
                rideId.isEmpty()) {

            startDriverLocationListener();

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

                                    readRideData(snapshot);

                                    startDriverLocationListener();
                                }
                        );
    }

    private void readRideData(
            DocumentSnapshot snapshot) {

        Double pickupLat =
                snapshot.getDouble(
                        "pickupLatitude"
                );

        Double pickupLon =
                snapshot.getDouble(
                        "pickupLongitude"
                );

        Double destinationLat =
                snapshot.getDouble(
                        "destinationLatitude"
                );

        Double destinationLon =
                snapshot.getDouble(
                        "destinationLongitude"
                );

        String firestoreDriverId =
                snapshot.getString(
                        "driverId"
                );

        if (pickupLat != null) {
            passengerLatitude = pickupLat;
        }

        if (pickupLon != null) {
            passengerLongitude = pickupLon;
        }

        if (destinationLat != null) {
            destinationLatitude = destinationLat;
        }

        if (destinationLon != null) {
            destinationLongitude = destinationLon;
        }

        if (firestoreDriverId != null &&
                !firestoreDriverId.isEmpty()) {

            driverId = firestoreDriverId;
        }

        updatePassengerOnMap();

        updateDestinationOnMap();

        requestPassengerDestinationRoute();

        fitMap();
    }

    private void startDriverLocationListener() {

        if (driverId == null ||
                driverId.isEmpty()) {

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

                                    driverLatitude = lat;
                                    driverLongitude = lon;

                                    updateDriverOnMap(
                                            lat,
                                            lon
                                    );

                                    maybeUpdateDriverRoute();

                                    fitMap();
                                }
                        );
    }

    private void updatePassengerOnMap() {

        if (!pageReady) return;

        String javascript =
                String.format(
                        Locale.US,
                        "setPassenger(%f,%f);",
                        passengerLatitude,
                        passengerLongitude
                );

        webView.evaluateJavascript(
                javascript,
                null
        );
    }

    private void updateDestinationOnMap() {

        if (!pageReady) return;

        String javascript =
                String.format(
                        Locale.US,
                        "setDestination(%f,%f);",
                        destinationLatitude,
                        destinationLongitude
                );

        webView.evaluateJavascript(
                javascript,
                null
        );
    }

    private void updateDriverOnMap(
            double latitude,
            double longitude) {

        if (!pageReady) return;

        String javascript =
                String.format(
                        Locale.US,
                        "setDriver(%f,%f);",
                        latitude,
                        longitude
                );

        webView.evaluateJavascript(
                javascript,
                null
        );
    }

    private void requestPassengerDestinationRoute() {

        if (!pageReady) return;

        if (!validCoordinate(
                passengerLatitude,
                passengerLongitude)) {
            return;
        }

        if (!validCoordinate(
                destinationLatitude,
                destinationLongitude)) {
            return;
        }

        final double startLat =
                passengerLatitude;

        final double startLon =
                passengerLongitude;

        final double endLat =
                destinationLatitude;

        final double endLon =
                destinationLongitude;

        routeExecutor.execute(() -> {

            RouteResult result =
                    getRoadRoute(
                            startLat,
                            startLon,
                            endLat,
                            endLon
                    );

            if (result == null) {

                runOnUiThread(() ->
                        setRoutingStatus(
                                "⚠️ Road route unavailable"
                        )
                );

                return;
            }

            runOnUiThread(() -> {

                drawPassengerRoute(
                        result.points,
                        result.distanceKm,
                        result.durationMin
                );
            });
        });
    }

    private void requestDriverPassengerRoute() {

        if (!pageReady) return;

        if (!validCoordinate(
                driverLatitude,
                driverLongitude)) {
            return;
        }

        if (!validCoordinate(
                passengerLatitude,
                passengerLongitude)) {
            return;
        }

        final double startLat =
                driverLatitude;

        final double startLon =
                driverLongitude;

        final double endLat =
                passengerLatitude;

        final double endLon =
                passengerLongitude;

        lastRouteDriverLatitude =
                startLat;

        lastRouteDriverLongitude =
                startLon;

        lastRouteRequestTime =
                System.currentTimeMillis();

        routeExecutor.execute(() -> {

            RouteResult result =
                    getRoadRoute(
                            startLat,
                            startLon,
                            endLat,
                            endLon
                    );

            if (result == null) {
                return;
            }

            runOnUiThread(() -> {

                drawDriverRoute(
                        result.points,
                        result.distanceKm,
                        result.durationMin
                );
            });
        });
    }

    private void maybeUpdateDriverRoute() {

        if (!pageReady) return;

        if (!validCoordinate(
                driverLatitude,
                driverLongitude)) {
            return;
        }

        long now =
                System.currentTimeMillis();

        if (now - lastRouteRequestTime <
                ROUTE_UPDATE_INTERVAL) {
            return;
        }

        double movedKm =
                distanceKm(
                        lastRouteDriverLatitude,
                        lastRouteDriverLongitude,
                        driverLatitude,
                        driverLongitude
                );

        if (lastRouteDriverLatitude == 0.0 ||
                lastRouteDriverLongitude == 0.0 ||
                movedKm >=
                        ROUTE_UPDATE_DISTANCE_KM) {

            requestDriverPassengerRoute();
        }
    }

    private RouteResult getRoadRoute(
            double startLat,
            double startLon,
            double endLat,
            double endLon) {

        HttpURLConnection connection =
                null;

        try {

            String routeUrl =
                    String.format(
                            Locale.US,
                            "https://router.project-osrm.org/" +
                            "route/v1/driving/" +
                            "%f,%f;%f,%f" +
                            "?overview=full" +
                            "&geometries=geojson",
                            startLon,
                            startLat,
                            endLon,
                            endLat
                    );

            URL url =
                    new URL(routeUrl);

            connection =
                    (HttpURLConnection)
                            url.openConnection();

            connection.setRequestMethod("GET");

            connection.setConnectTimeout(10000);

            connection.setReadTimeout(15000);

            connection.setRequestProperty(
                    "User-Agent",
                    "SakayNa/1.0 Android"
            );

            int responseCode =
                    connection.getResponseCode();

            if (responseCode !=
                    HttpURLConnection.HTTP_OK) {

                return null;
            }

            InputStream input =
                    connection.getInputStream();

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    input
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

            JSONArray routes =
                    root.getJSONArray("routes");

            if (routes.length() == 0) {
                return null;
            }

            JSONObject route =
                    routes.getJSONObject(0);

            double distanceMeters =
                    route.getDouble(
                            "distance"
                    );

            double durationSeconds =
                    route.getDouble(
                            "duration"
                    );

            JSONObject geometry =
                    route.getJSONObject(
                            "geometry"
                    );

            JSONArray coordinates =
                    geometry.getJSONArray(
                            "coordinates"
                    );

            StringBuilder points =
                    new StringBuilder();

            points.append("[");

            for (int i = 0;
                    i < coordinates.length();
                    i++) {

                JSONArray coordinate =
                        coordinates.getJSONArray(i);

                double lon =
                        coordinate.getDouble(0);

                double lat =
                        coordinate.getDouble(1);

                if (i > 0) {
                    points.append(",");
                }

                points.append("[")
                        .append(
                                String.format(
                                        Locale.US,
                                        "%.6f",
                                        lat
                                )
                        )
                        .append(",")
                        .append(
                                String.format(
                                        Locale.US,
                                        "%.6f",
                                        lon
                                )
                        )
                        .append("]");
            }

            points.append("]");

            RouteResult result =
                    new RouteResult();

            result.points =
                    points.toString();

            result.distanceKm =
                    distanceMeters / 1000.0;

            result.durationMin =
                    durationSeconds / 60.0;

            return result;

        } catch (Exception e) {

            return null;

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void drawDriverRoute(
            String points,
            double distanceKm,
            double durationMin) {

        if (!pageReady) return;

        String javascript =
                "drawDriverRoute(" +
                points +
                ");" +

                "updateDriverRouteInfo(" +
                String.format(
                        Locale.US,
                        "%.3f,%.1f",
                        distanceKm,
                        durationMin
                ) +
                ");";

        webView.evaluateJavascript(
                javascript,
                null
        );
    }

    private void drawPassengerRoute(
            String points,
            double distanceKm,
            double durationMin) {

        if (!pageReady) return;

        String javascript =
                "drawPassengerRoute(" +
                points +
                ");" +

                "updatePassengerRouteInfo(" +
                String.format(
                        Locale.US,
                        "%.3f,%.1f",
                        distanceKm,
                        durationMin
                ) +
                ");";

        webView.evaluateJavascript(
                javascript,
                null
        );
    }

    private boolean validCoordinate(
            double latitude,
            double longitude) {

        return latitude >= -90.0 &&
                latitude <= 90.0 &&
                longitude >= -180.0 &&
                longitude <= 180.0 &&
                !(latitude == 0.0 &&
                        longitude == 0.0);
    }

    private double distanceKm(
            double lat1,
            double lon1,
            double lat2,
            double lon2) {

        if (!validCoordinate(lat1, lon1) ||
                !validCoordinate(lat2, lon2)) {
            return 0.0;
        }

        double earthRadius = 6371.0;

        double dLat =
                Math.toRadians(lat2 - lat1);

        double dLon =
                Math.toRadians(lon2 - lon1);

        double a =
                Math.sin(dLat / 2) *
                Math.sin(dLat / 2) +

                Math.cos(
                        Math.toRadians(lat1)
                ) *

                Math.cos(
                        Math.toRadians(lat2)
                ) *

                Math.sin(dLon / 2) *
                Math.sin(dLon / 2);

        double c =
                2 *
                Math.atan2(
                        Math.sqrt(a),
                        Math.sqrt(1 - a)
                );

        return earthRadius * c;
    }

    private void fitMap() {

        if (!pageReady) return;

        webView.evaluateJavascript(
                "fitAll();",
                null
        );
    }

    @Override
    protected void onDestroy() {

        if (driverLocationListener != null) {
            driverLocationListener.remove();
        }

        if (rideListener != null) {
            rideListener.remove();
        }

        routeExecutor.shutdownNow();

        if (webView != null) {
            webView.destroy();
        }

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {

        if (webView != null &&
                webView.canGoBack()) {

            webView.goBack();

        } else {

            super.onBackPressed();
        }
    }

    private static class RouteResult {

        String points;

        double distanceKm;

        double durationMin;
    }
}
