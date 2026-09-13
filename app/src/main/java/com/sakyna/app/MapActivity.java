
package com.sakyna.app;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class MapActivity extends Activity {

    private WebView webView;

    private FirebaseFirestore db;
    private ListenerRegistration driverLocationListener;

    private String rideId = "";
    private String driverId = "";

    private boolean mapReady = false;

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
                "var firstDriverLocation = true;" +

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

                "   if (firstDriverLocation) {" +
                "       map.setView([lat,lng],16);" +
                "       firstDriverLocation = false;" +
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

        webView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mapReady = true;
                loadCurrentRide();
            }
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

                    String foundDriverId =
                            document.getString("driverId");

                    if (foundDriverId == null ||
                            foundDriverId.trim().isEmpty()) {

                        Toast.makeText(
                                this,
                                "Driver has not been assigned yet.",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    driverId = foundDriverId;

                    startDriverLocationListener();

                })
                .addOnFailureListener(error -> {

                    Toast.makeText(
                            this,
                            "Unable to load ride.",
                            Toast.LENGTH_LONG
                    ).show();
                });
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
