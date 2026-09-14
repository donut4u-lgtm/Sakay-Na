package com.sakyna.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Locale;

public class MapActivity extends Activity {

    private WebView webView;
    private TextView statusText;

    private FirebaseFirestore db;
    private ListenerRegistration rideListener;

    private double pickupLat;
    private double pickupLng;

    private double destinationLat;
    private double destinationLng;

    private boolean destinationSelected = false;

    private String mode = "";
    private String rideId = "";

    private Button confirmButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();

        Intent intent = getIntent();

        mode = intent.getStringExtra("mode");
        if (mode == null) {
            mode = "";
        }

        rideId = intent.getStringExtra("ride_id");
        if (rideId == null) {
            rideId = "";
        }

        pickupLat = intent.getDoubleExtra(
                "passenger_latitude",
                0.0
        );

        pickupLng = intent.getDoubleExtra(
                "passenger_longitude",
                0.0
        );

        buildScreen();

        if ("LIVE_RIDE".equals(mode)
                || !rideId.isEmpty()) {

            setupLiveRide();

        } else {

            setupDestinationSelection();
        }
    }

    private void buildScreen() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);

        if ("LIVE_RIDE".equals(mode)
                || !rideId.isEmpty()) {

            title.setText("🗺️ LIVE RIDE MAP");

        } else {

            title.setText("🗺️ CHOOSE DESTINATION");
        }

        title.setTextSize(21);
        title.setTextColor(Color.rgb(0, 110, 70));
        title.setGravity(Gravity.CENTER);
        title.setPadding(10, 20, 10, 15);

        root.addView(
                title,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        statusText = new TextView(this);
        statusText.setTextSize(16);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(10, 5, 10, 15);

        root.addView(
                statusText,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        webView = new WebView(this);

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setLoadWithOverviewMode(false);
        settings.setUseWideViewPort(true);

        webView.setWebViewClient(new WebViewClient());

        root.addView(
                webView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        if (!"LIVE_RIDE".equals(mode)
                && rideId.isEmpty()) {

            confirmButton = new Button(this);

            confirmButton.setText(
                    "✅ CONFIRM DESTINATION"
            );

            confirmButton.setTextSize(17);
            confirmButton.setEnabled(false);

            confirmButton.setOnClickListener(
                    v -> confirmDestination()
            );

            root.addView(
                    confirmButton,
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            );

        } else {

            Button closeButton = new Button(this);

            closeButton.setText(
                    "⬅️ BACK"
            );

            closeButton.setOnClickListener(
                    v -> finish()
            );

            root.addView(
                    closeButton,
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            );
        }

        setContentView(root);
    }

    private void setupDestinationSelection() {

        if (pickupLat == 0.0
                && pickupLng == 0.0) {

            statusText.setText(
                    "⚠️ Pickup location unavailable."
            );

            Toast.makeText(
                    this,
                    "Your current location was not received.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        statusText.setText(
                "👆 Tap the map to choose your destination."
        );

        loadDestinationMap();
    }

    private void loadDestinationMap() {

        String html = createDestinationMapHtml();

        webView.addJavascriptInterface(
                new DestinationBridge(),
                "Android"
        );

        webView.loadDataWithBaseURL(
                "https://sakyna.app/",
                html,
                "text/html",
                "UTF-8",
                null
        );
    }

    private String createDestinationMapHtml() {

        String lat =
                String.format(
                        Locale.US,
                        "%.8f",
                        pickupLat
                );

        String lng =
                String.format(
                        Locale.US,
                        "%.8f",
                        pickupLng
                );

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +

                "<meta name='viewport' " +
                "content='width=device-width, initial-scale=1.0'>" +

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

                ".leaflet-control-attribution{" +
                "font-size:9px;" +
                "}" +

                "</style>" +

                "</head>" +

                "<body>" +

                "<div id='map'></div>" +

                "<script>" +

                "var pickupLat=" + lat + ";" +
                "var pickupLng=" + lng + ";" +

                "var map=L.map('map').setView(" +
                "[pickupLat,pickupLng],16);" +

                "L.tileLayer(" +
                "'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'," +
                "{" +
                "maxZoom:19," +
                "attribution:'© OpenStreetMap contributors'" +
                "}" +
                ").addTo(map);" +

                "var pickupMarker=L.marker(" +
                "[pickupLat,pickupLng]" +
                ").addTo(map);" +

                "pickupMarker.bindPopup(" +
                "'📍 YOUR PICKUP LOCATION'" +
                ").openPopup();" +

                "var destinationMarker=null;" +

                "map.on('click',function(e){" +

                "var lat=e.latlng.lat;" +
                "var lng=e.latlng.lng;" +

                "if(destinationMarker!==null){" +
                "map.removeLayer(destinationMarker);" +
                "}" +

                "destinationMarker=L.marker([lat,lng]).addTo(map);" +

                "destinationMarker.bindPopup(" +
                "'🏁 YOUR DESTINATION'" +
                ").openPopup();" +

                "Android.destinationSelected(lat,lng);" +

                "});" +

                "</script>" +

                "</body>" +
                "</html>";
    }

    private class DestinationBridge {

        @JavascriptInterface
        public void destinationSelected(
                double lat,
                double lng) {

            runOnUiThread(() -> {

                destinationLat = lat;
                destinationLng = lng;

                destinationSelected = true;

                if (confirmButton != null) {
                    confirmButton.setEnabled(true);
                }

                statusText.setText(
                        String.format(
                                Locale.US,
                                "🏁 Destination selected\n%.6f, %.6f",
                                lat,
                                lng
                        )
                );
            });
        }
    }

    private void confirmDestination() {

        if (!destinationSelected) {

            Toast.makeText(
                    this,
                    "Please tap the map first.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Intent result = new Intent();

        result.putExtra(
                "destination_latitude",
                destinationLat
        );

        result.putExtra(
                "destination_longitude",
                destinationLng
        );

        result.putExtra(
                "destination_address",
                String.format(
                        Locale.US,
                        "Selected location (%.6f, %.6f)",
                        destinationLat,
                        destinationLng
                )
        );

        setResult(
                RESULT_OK,
                result
        );

        finish();
    }

    private void setupLiveRide() {

        statusText.setText(
                "⏳ Connecting to live ride..."
        );

        if (rideId == null
                || rideId.isEmpty()) {

            statusText.setText(
                    "⚠️ Ride ID unavailable."
            );

            return;
        }

        loadLiveMap();

        rideListener =
                db.collection("rides")
                        .document(rideId)
                        .addSnapshotListener(
                                (snapshot, error) -> {

                                    if (error != null) {

                                        statusText.setText(
                                                "⚠️ Unable to update live ride."
                                        );

                                        return;
                                    }

                                    if (snapshot == null
                                            || !snapshot.exists()) {

                                        statusText.setText(
                                                "⚠️ Ride not found."
                                        );

                                        return;
                                    }

                                    updateLiveRide(snapshot);
                                }
                        );
    }

    private void loadLiveMap() {

        String startLat =
                String.format(
                        Locale.US,
                        "%.8f",
                        pickupLat == 0.0
                                ? 14.5995
                                : pickupLat
                );

        String startLng =
                String.format(
                        Locale.US,
                        "%.8f",
                        pickupLng == 0.0
                                ? 120.9842
                                : pickupLng
                );

        String html =
                createLiveMapHtml(
                        startLat,
                        startLng
                );

        webView.addJavascriptInterface(
                new LiveMapBridge(),
                "Android"
        );

        webView.loadDataWithBaseURL(
                "https://sakyna.app/",
                html,
                "text/html",
                "UTF-8",
                null
        );
    }

    private String createLiveMapHtml(
            String startLat,
            String startLng) {

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +

                "<meta name='viewport' " +
                "content='width=device-width, initial-scale=1.0'>" +

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

                "var map=L.map('map').setView([" +
                startLat + "," +
                startLng + "],15);" +

                "L.tileLayer(" +
                "'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'," +
                "{" +
                "maxZoom:19," +
                "attribution:'© OpenStreetMap contributors'" +
                "}" +
                ").addTo(map);" +

                "var passengerMarker=L.marker([" +
                startLat + "," +
                startLng +
                "]).addTo(map);" +

                "passengerMarker.bindPopup(" +
                "'📍 Passenger pickup'" +
                ");" +

                "var driverMarker=null;" +

                "function updateDriver(lat,lng){" +

                "if(driverMarker===null){" +

                "driverMarker=L.marker([lat,lng])" +
                ".addTo(map);" +

                "driverMarker.bindPopup(" +
                "'🛺 Driver location'" +
                ").openPopup();" +

                "}else{" +

                "driverMarker.setLatLng([lat,lng]);" +

                "}" +

                "map.panTo([lat,lng]);" +

                "}" +

                "</script>" +

                "</body>" +
                "</html>";
    }

    private class LiveMapBridge {

        @JavascriptInterface
        public void updateDriverLocation(
                double lat,
                double lng) {

            runOnUiThread(() -> {

                if (webView != null) {

                    String javascript =
                            "updateDriver(" +
                                    lat +
                                    "," +
                                    lng +
                                    ");";

                    webView.evaluateJavascript(
                            javascript,
                            null
                    );
                }
            });
        }
    }

    private void updateLiveRide(
            DocumentSnapshot snapshot) {

        String status =
                snapshot.getString("status");

        if (status == null) {
            status = "UNKNOWN";
        }

        statusText.setText(
                "Ride status: " + status
        );

        Double driverLat =
                getDouble(snapshot, "driverLatitude");

        Double driverLng =
                getDouble(snapshot, "driverLongitude");

        if (driverLat != null
                && driverLng != null
                && driverLat != 0.0
                && driverLng != 0.0) {

            String javascript =
                    "updateDriver(" +
                            driverLat +
                            "," +
                            driverLng +
                            ");";

            if (webView != null) {

                webView.evaluateJavascript(
                        javascript,
                        null
                );
            }

            statusText.setText(
                    String.format(
                            Locale.US,
                            "🛺 Driver location\n%.6f, %.6f\nStatus: %s",
                            driverLat,
                            driverLng,
                            status
                    )
            );
        }

        if ("COMPLETED".equals(status)) {

            statusText.setText(
                    "✅ Ride completed."
            );
        }

        if ("CANCELLED".equals(status)) {

            statusText.setText(
                    "❌ Ride cancelled."
            );
        }
    }

    private Double getDouble(
            DocumentSnapshot snapshot,
            String field) {

        Object value =
                snapshot.get(field);

        if (value instanceof Number) {

            return ((Number) value).doubleValue();
        }

        return null;
    }

    @Override
    protected void onDestroy() {

        if (rideListener != null) {
            rideListener.remove();
            rideListener = null;
        }

        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }
}
