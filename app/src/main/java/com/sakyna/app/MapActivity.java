
package com.sakyna.app;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Locale;

public class MapActivity extends Activity {

    private WebView webView;
    private FirebaseFirestore db;

    private ListenerRegistration driverLocationListener;
    private ListenerRegistration rideListener;

    private String rideId = "";
    private String driverId = "";

    private double passengerLatitude = 14.2456;
    private double passengerLongitude = 121.4451;

    private double destinationLatitude = 14.2456;
    private double destinationLongitude = 121.4451;

    private double driverLatitude = 0.0;
    private double driverLongitude = 0.0;

    private boolean pageReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();

        webView = new WebView(this);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        webView.setWebViewClient(new WebViewClient());

        setContentView(webView);

        readIntentData();
        loadMap();
        listenToRide();
    }

    private void readIntentData() {

        if (getIntent().hasExtra("ride_id")) {
            rideId = getIntent().getStringExtra("ride_id");
        }

        if (getIntent().hasExtra("driver_id")) {
            driverId = getIntent().getStringExtra("driver_id");
        }

        if (getIntent().hasExtra("passenger_latitude")) {
            passengerLatitude =
                    getIntent().getDoubleExtra(
                            "passenger_latitude",
                            passengerLatitude
                    );
        }

        if (getIntent().hasExtra("passenger_longitude")) {
            passengerLongitude =
                    getIntent().getDoubleExtra(
                            "passenger_longitude",
                            passengerLongitude
                    );
        }

        if (getIntent().hasExtra("destination_latitude")) {
            destinationLatitude =
                    getIntent().getDoubleExtra(
                            "destination_latitude",
                            destinationLatitude
                    );
        }

        if (getIntent().hasExtra("destination_longitude")) {
            destinationLongitude =
                    getIntent().getDoubleExtra(
                            "destination_longitude",
                            destinationLongitude
                    );
        }

        if (getIntent().hasExtra("driver_latitude")) {
            driverLatitude =
                    getIntent().getDoubleExtra(
                            "driver_latitude",
                            0.0
                    );
        }

        if (getIntent().hasExtra("driver_longitude")) {
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
                "padding:12px;" +
                "border-radius:12px;" +
                "box-shadow:0 2px 8px rgba(0,0,0,.3);" +
                "font-family:Arial;" +
                "font-size:15px;" +
                "}" +

                "</style>" +

                "<link rel='stylesheet' " +
                "href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'>" +

                "</head>" +

                "<body>" +

                "<div class='infoBox'>" +
                "<b>🚖 SAKAY NA</b><br>" +
                "<span id='status'>Waiting for driver location...</span>" +
                "</div>" +

                "<div id='map'></div>" +

                "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'>" +
                "</script>" +

                "<script>" +

                "var map = L.map('map').setView([14.2456,121.4451],13);" +

                "L.tileLayer(" +
                "'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'," +
                "{" +
                "maxZoom:19," +
                "attribution:'© OpenStreetMap contributors'" +
                "}" +
                ").addTo(map);" +

                "var passengerMarker = null;" +
                "var driverMarker = null;" +
                "var destinationMarker = null;" +
                "var routeLine = null;" +

                "var passengerIcon = L.divIcon({" +
                "className:'',"+
                "html:'<div style=\"font-size:32px\">👤</div>'," +
                "iconSize:[40,40]," +
                "iconAnchor:[20,20]" +
                "});" +

                "var driverIcon = L.divIcon({" +
                "className:'',"+
                "html:'<div style=\"font-size:34px\">🛺</div>'," +
                "iconSize:[40,40]," +
                "iconAnchor:[20,20]" +
                "});" +

                "var destinationIcon = L.divIcon({" +
                "className:'',"+
                "html:'<div style=\"font-size:32px\">📍</div>'," +
                "iconSize:[40,40]," +
                "iconAnchor:[20,40]" +
                "});" +

                "function setPassenger(lat,lon) {" +

                "if(passengerMarker==null) {" +
                "passengerMarker=L.marker([lat,lon],{" +
                "icon:passengerIcon" +
                "}).addTo(map)" +
                ".bindPopup('<b>Passenger</b><br>Pickup location');" +
                "} else {" +
                "passengerMarker.setLatLng([lat,lon]);" +
                "}" +

                "}" +

                "function setDestination(lat,lon) {" +

                "if(destinationMarker==null) {" +
                "destinationMarker=L.marker([lat,lon],{" +
                "icon:destinationIcon" +
                "}).addTo(map)" +
                ".bindPopup('<b>Destination</b>');" +
                "} else {" +
                "destinationMarker.setLatLng([lat,lon]);" +
                "}" +

                "drawRoute();" +

                "}" +

                "function setDriver(lat,lon) {" +

                "if(driverMarker==null) {" +
                "driverMarker=L.marker([lat,lon],{" +
                "icon:driverIcon" +
                "}).addTo(map)" +
                ".bindPopup('<b>Sakay Na Driver</b><br>Live location');" +
                "} else {" +
                "driverMarker.setLatLng([lat,lon]);" +
                "}" +

                "document.getElementById('status').innerHTML=" +
                "'🟢 Driver is live';" +

                "drawRoute();" +

                "}" +

                "function drawRoute() {" +

                "var points=[];" +

                "if(driverMarker!=null) {" +
                "points.push(driverMarker.getLatLng());" +
                "}" +

                "if(passengerMarker!=null) {" +
                "points.push(passengerMarker.getLatLng());" +
                "}" +

                "if(destinationMarker!=null) {" +
                "points.push(destinationMarker.getLatLng());" +
                "}" +

                "if(routeLine!=null) {" +
                "map.removeLayer(routeLine);" +
                "}" +

                "if(points.length>=2) {" +

                "routeLine=L.polyline(points,{" +
                "weight:5," +
                "opacity:0.8" +
                "}).addTo(map);" +

                "}" +

                "}" +

                "function fitAll() {" +

                "var points=[];" +

                "if(passengerMarker!=null)" +
                "points.push(passengerMarker.getLatLng());" +

                "if(driverMarker!=null)" +
                "points.push(driverMarker.getLatLng());" +

                "if(destinationMarker!=null)" +
                "points.push(destinationMarker.getLatLng());" +

                "if(points.length>0) {" +
                "map.fitBounds(points,{padding:[50,50]});" +
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

            fitMap();

        }, 1500);
    }

    private void listenToRide() {

        if (rideId == null || rideId.isEmpty()) {

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
                snapshot.getDouble("pickupLatitude");

        Double pickupLon =
                snapshot.getDouble("pickupLongitude");

        Double destinationLat =
                snapshot.getDouble("destinationLatitude");

        Double destinationLon =
                snapshot.getDouble("destinationLongitude");

        String firestoreDriverId =
                snapshot.getString("driverId");

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
}
