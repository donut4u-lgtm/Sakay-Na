
package com.sakyna.app;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MapActivity extends Activity {

    private WebView webView;

    private FirebaseFirestore db;
    private ListenerRegistration driverLocationListener;

    private String rideId = "";
    private String driverId = "";

    private boolean mapReady = false;

    private double pickupLatitude = 0.0;
    private double pickupLongitude = 0.0;

    private double destinationLatitude = 0.0;
    private double destinationLongitude = 0.0;

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

        String html =
                "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +

                "<style>" +
                "html,body,#map{height:100%;margin:0;padding:0;}" +
                "</style>" +

                "<link rel='stylesheet' " +
                "href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'>" +

                "</head>" +

                "<body>" +

                "<div id='map'></div>" +

                "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>" +

                "<script>" +

                "var map = L.map('map').setView([14.2456,121.4451],13);" +

                "L.tileLayer(" +
                "'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'," +
                "{maxZoom:19," +
                "attribution:'© OpenStreetMap contributors'}" +
                ").addTo(map);" +

                "var driverMarker = null;" +
                "var pickupMarker = null;" +
                "var destinationMarker = null;" +
                "var routeLine = null;" +

                "function setPickup(lat,lng) {" +
                "   if (pickupMarker === null) {" +
                "       pickupMarker = L.marker([lat,lng])" +
                "       .addTo(map)" +
                "       .bindPopup('<b>Pickup A</b>');" +
                "   } else {" +
                "       pickupMarker.setLatLng([lat,lng]);" +
                "   }" +
                "}" +

                "function setDestination(lat,lng) {" +
                "   if (destinationMarker === null) {" +
                "       destinationMarker = L.marker([lat,lng])" +
                "       .addTo(map)" +
                "       .bindPopup('<b>Destination B</b>');" +
                "   } else {" +
                "       destinationMarker.setLatLng([lat,lng]);" +
                "   }" +
                "}" +

                "function drawRoute(points) {" +

                "   if (routeLine !== null) {" +
                "       map.removeLayer(routeLine);" +
                "   }" +

                "   routeLine = L.polyline(points, {" +
                "       weight:6," +
                "       opacity:0.8" +
                "   }).addTo(map);" +

                "   if (routeLine.getBounds().isValid()) {" +
                "       map.fitBounds(routeLine.getBounds(), {" +
                "           padding:[30,30]" +
                "       });" +
                "   }" +
                "}" +

                "function updateDriver(lat,lng) {" +

                "   lat = Number(lat);" +
                "   lng = Number(lng);" +

                "   if (isNaN(lat) || isNaN(lng)) return;" +

                "   if (driverMarker === null) {" +

                "       driverMarker = L.marker([lat,lng])" +
                "       .addTo(map)" +
                "       .bindPopup('<b>Sakay Na Driver</b><br>Live location');" +

                "   } else {" +

                "       driverMarker.setLatLng([lat,lng]);" +
                "   }" +
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

        setContentView(webView);

        webView.postDelayed(() -> {
            mapReady = true;
            loadCurrentRide();
        }, 1000);
    }

    private void loadCurrentRide() {

        android.content.SharedPreferences prefs =
                getSharedPreferences("SakayNa", MODE_PRIVATE);

        rideId = prefs.getString("ride_id", "");

        if (rideId == null || rideId.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "No active ride found.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        db.collection("rides")
                .document(rideId)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {

                        Toast.makeText(
                                this,
                                "Ride not found.",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    Double pickupLat =
                            document.getDouble("pickupLatitude");

                    Double pickupLng =
                            document.getDouble("pickupLongitude");

                    Double destinationLat =
                            document.getDouble("destinationLatitude");

                    Double destinationLng =
                            document.getDouble("destinationLongitude");

                    if (pickupLat == null ||
                            pickupLng == null ||
                            destinationLat == null ||
                            destinationLng == null) {

                        Toast.makeText(
                                this,
                                "Pickup or destination coordinates are missing.",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    pickupLatitude = pickupLat;
                    pickupLongitude = pickupLng;

                    destinationLatitude = destinationLat;
                    destinationLongitude = destinationLng;

                    showPickupAndDestination();

                    drawRoute();

                    String foundDriverId =
                            document.getString("driverId");

                    if (foundDriverId != null &&
                            !foundDriverId.trim().isEmpty()) {

                        driverId = foundDriverId;

                        startDriverLocationListener();
                    }

                })
                .addOnFailureListener(error -> {

                    Toast.makeText(
                            this,
                            "Unable to load ride.",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void showPickupAndDestination() {

        if (!mapReady || webView == null) {
            return;
        }

        String javascript =
                "setPickup(" +
                        pickupLatitude +
                        "," +
                        pickupLongitude +
                        ");" +

                "setDestination(" +
                        destinationLatitude +
                        "," +
                        destinationLongitude +
                        ");";

        webView.post(() ->
                webView.evaluateJavascript(
                        javascript,
                        null
                )
        );
    }

    private void drawRoute() {

        Thread routeThread = new Thread(() -> {

            HttpURLConnection connection = null;

            try {

                String urlString =
                        "https://router.project-osrm.org/route/v1/driving/" +
                        pickupLongitude + "," +
                        pickupLatitude + ";" +
                        destinationLongitude + "," +
                        destinationLatitude +
                        "?overview=full&geometries=geojson";

                URL url = new URL(urlString);

                connection =
                        (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        connection.getInputStream()
                                )
                        );

                StringBuilder response =
                        new StringBuilder();

                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();

                JSONObject json =
                        new JSONObject(response.toString());

                JSONArray routes =
                        json.getJSONArray("routes");

                if (routes.length() == 0) {
                    return;
                }

                JSONObject route =
                        routes.getJSONObject(0);

                JSONObject geometry =
                        route.getJSONObject("geometry");

                JSONArray coordinates =
                        geometry.getJSONArray("coordinates");

                JSONArray leafletPoints =
                        new JSONArray();

                for (int i = 0;
                     i < coordinates.length();
                     i++) {

                    JSONArray point =
                            coordinates.getJSONArray(i);

                    double longitude =
                            point.getDouble(0);

                    double latitude =
                            point.getDouble(1);

                    JSONArray leafletPoint =
                            new JSONArray();

                    leafletPoint.put(latitude);
                    leafletPoint.put(longitude);

                    leafletPoints.put(leafletPoint);
                }

                runOnUiThread(() -> {

                    if (!mapReady || webView == null) {
                        return;
                    }

                    String javascript =
                            "drawRoute(" +
                                    leafletPoints.toString() +
                                    ");";

                    webView.evaluateJavascript(
                            javascript,
                            null
                    );
                });

            } catch (Exception ignored) {

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

        });

        routeThread.start();
    }

    private void startDriverLocationListener() {

        if (driverLocationListener != null) {
            driverLocationListener.remove();
        }

        driverLocationListener =
                db.collection("driverLocations")
                        .document(driverId)
                        .addSnapshotListener((snapshot, error) -> {

                            if (error != null) {
                                return;
                            }

                            if (snapshot == null ||
                                    !snapshot.exists()) {

                                return;
                            }

                            Double latitude =
                                    snapshot.getDouble("latitude");

                            Double longitude =
                                    snapshot.getDouble("longitude");

                            if (latitude == null ||
                                    longitude == null) {

                                return;
                            }

                            updateDriverMarker(
                                    latitude,
                                    longitude
                            );
                        });
    }

    private void updateDriverMarker(
            double latitude,
            double longitude) {

        if (!mapReady || webView == null) {
            return;
        }

        String javascript =
                "updateDriver(" +
                        latitude +
                        "," +
                        longitude +
                        ");";

        webView.post(() ->
                webView.evaluateJavascript(
                        javascript,
                        null
                )
        );
    }

    @Override
    protected void onDestroy() {

        if (driverLocationListener != null) {
            driverLocationListener.remove();
            driverLocationListener = null;
        }

        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {

        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
