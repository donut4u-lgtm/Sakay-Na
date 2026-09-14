
package com.sakyna.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.Gravity;
import android.view.ViewGroup;

public class MapActivity extends Activity {

    private WebView webView;
    private TextView statusText;

    private double pickupLat;
    private double pickupLng;

    private double destinationLat;
    private double destinationLng;

    private boolean destinationSelected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pickupLat = getIntent().getDoubleExtra(
                "passenger_latitude", 0
        );

        pickupLng = getIntent().getDoubleExtra(
                "passenger_longitude", 0
        );

        buildScreen();
    }

    private void buildScreen() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("🗺️ CHOOSE DESTINATION");
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
        statusText.setText(
                "👆 Tap the map to choose your destination."
        );
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

        webView.setWebViewClient(new WebViewClient());

        root.addView(
                webView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        Button confirmButton = new Button(this);
        confirmButton.setText("✅ CONFIRM DESTINATION");
        confirmButton.setTextSize(17);
        confirmButton.setEnabled(false);

        confirmButton.setOnClickListener(v -> confirmDestination());

        root.addView(
                confirmButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        setContentView(root);

        loadMap(confirmButton);
    }

    private void loadMap(Button confirmButton) {

        String html = createMapHtml();

        webView.setWebViewClient(
                new WebViewClient() {

                    @Override
                    public void onPageFinished(
                            WebView view,
                            String url) {

                        super.onPageFinished(view, url);

                        statusText.setText(
                                "👆 Tap the map to choose your destination."
                        );
                    }
                }
        );

        webView.loadDataWithBaseURL(
                "https://sakyna.app/",
                html,
                "text/html",
                "UTF-8",
                null
        );

        webView.addJavascriptInterface(
                new MapBridge(confirmButton),
                "Android"
        );
    }

    private String createMapHtml() {

        String lat =
                String.valueOf(
                        pickupLat == 0 ? 14.0 : pickupLat
                );

        String lng =
                String.valueOf(
                        pickupLng == 0 ? 121.0 : pickupLng
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
                "[pickupLat,pickupLng],15);" +

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
                "'📍 Pickup location'" +
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
                "'🔴 Destination'" +
                ").openPopup();" +

                "Android.destinationSelected(lat,lng);" +

                "});" +

                "</script>" +

                "</body>" +
                "</html>";
    }

    private class MapBridge {

        private final Button confirmButton;

        MapBridge(Button button) {
            confirmButton = button;
        }

        @android.webkit.JavascriptInterface
        public void destinationSelected(
                double lat,
                double lng) {

            runOnUiThread(() -> {

                destinationLat = lat;
                destinationLng = lng;

                destinationSelected = true;

                confirmButton.setEnabled(true);

                statusText.setText(
                        String.format(
                                java.util.Locale.US,
                                "🔴 Destination selected\n%.6f, %.6f",
                                lat,
                                lng
                        )
                );
            });
        }
    }

    private void confirmDestination() {

        if (!destinationSelected) {
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
                        java.util.Locale.US,
                        "Selected map location (%.6f, %.6f)",
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

    @Override
    protected void onDestroy() {

        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }
}
