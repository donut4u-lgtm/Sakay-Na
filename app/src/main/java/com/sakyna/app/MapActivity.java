
package com.sakyna.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class MapActivity extends Activity {

    private static final int LOCATION_REQUEST = 8001;

    private WebView webView;
    private TextView titleText;
    private TextView selectedText;

    private LocationManager locationManager;
    private Location currentLocation;

    private FirebaseFirestore db;
    private ListenerRegistration rideListener;

    private String rideId;
    private String mode;

    private double pickupLatitude;
    private double pickupLongitude;

    private double destinationLatitude;
    private double destinationLongitude;

    private String destinationAddress = "";

    private boolean mapReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();

        rideId = getIntent().getStringExtra("rideId");
        mode = getIntent().getStringExtra("mode");

        if (mode == null) {
            mode = "";
        }

        pickupLatitude =
                getIntent().getDoubleExtra(
                        "pickup_latitude", 0.0);

        pickupLongitude =
                getIntent().getDoubleExtra(
                        "pickup_longitude", 0.0);

        destinationLatitude =
                getIntent().getDoubleExtra(
                        "destination_latitude", 0.0);

        destinationLongitude =
                getIntent().getDoubleExtra(
                        "destination_longitude", 0.0);

        destinationAddress =
                getIntent().getStringExtra(
                        "destination_address");

        if (destinationAddress == null) {
            destinationAddress = "";
        }

        buildScreen();
        setupLocation();

        if ("SELECT_DESTINATION".equals(mode)) {
            setupDestinationMode();
        } else if (rideId != null && !rideId.isEmpty()) {
            setupLiveRideMode();
        } else {
            setupDestinationMode();
        }
    }

    private void buildScreen() {

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL);

        root.setBackgroundColor(Color.WHITE);

        titleText =
                new TextView(this);

        titleText.setTextSize(20);
        titleText.setGravity(Gravity.CENTER);
        titleText.setPadding(
                12, 14, 12, 14);

        root.addView(
                titleText,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

        selectedText =
                new TextView(this);

        selectedText.setTextSize(15);
        selectedText.setPadding(
                16, 8, 16, 8);

        root.addView(
                selectedText,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

        webView =
                new WebView(this);

        LinearLayout.LayoutParams mapParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1);

        root.addView(
                webView,
                mapParams);

        LinearLayout buttonRow =
                new LinearLayout(this);

        buttonRow.setOrientation(
                LinearLayout.HORIZONTAL);

        Button myLocation =
                new Button(this);

        myLocation.setText(
                "My Location");

        buttonRow.addView(
                myLocation,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1));

        Button confirm =
                new Button(this);

        confirm.setText(
                "Confirm Destination");

        buttonRow.addView(
                confirm,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1));

        root.addView(buttonRow);

        Button back =
                new Button(this);

        back.setText("Back");

        root.addView(
                back,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(root);

        myLocation.setOnClickListener(
                v -> centerOnMyLocation());

        confirm.setOnClickListener(
                v -> confirmDestination());

        back.setOnClickListener(
                v -> finish());

        setupWebView();
    }

    private void setupDestinationMode() {

        titleText.setText(
                "Choose Your Destination");

        selectedText.setText(
                "TAP THE MAP TO CHOOSE YOUR DESTINATION");
    }

    private void setupLiveRideMode() {

        titleText.setText(
                "Live Ride Map");

        selectedText.setText(
                "Driver location will appear on the map.");

        listenForRide();
    }

    private void setupWebView() {

        WebSettings settings =
                webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setBlockNetworkImage(false);
        settings.setBlockNetworkLoads(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setUseWideViewPort(false);
        settings.setLoadWithOverviewMode(false);

        webView.setWebViewClient(
                new WebViewClient());

        webView.setWebChromeClient(
                new WebChromeClient());

        webView.addJavascriptInterface(
                new MapBridge(),
                "Android");

        webView.setBackgroundColor(
                Color.rgb(225, 225, 225));

        loadMapHtml();
    }

    private void loadMapHtml() {

        double startLat =
                getInitialLatitude();

        double startLng =
                getInitialLongitude();

        String html =
                "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +

                "<meta name='viewport' " +
                "content='width=device-width," +
                "initial-scale=1.0," +
                "maximum-scale=1.0," +
                "user-scalable=no'>" +

                "<style>" +

                "html,body{" +
                "margin:0;" +
                "padding:0;" +
                "width:100%;" +
                "height:100%;" +
                "overflow:hidden;" +
                "background:#d9d9d9;" +
                "}" +

                "#map{" +
                "position:absolute;" +
                "left:0;" +
                "top:0;" +
                "width:100%;" +
                "height:100%;" +
                "overflow:hidden;" +
                "background:#d9d9d9;" +
                "touch-action:none;" +
                "}" +

                "#tiles{" +
                "position:absolute;" +
                "left:0;" +
                "top:0;" +
                "width:100%;" +
                "height:100%;" +
                "z-index:1;" +
                "pointer-events:none;" +
                "}" +

                ".tile{" +
                "position:absolute;" +
                "width:256px;" +
                "height:256px;" +
                "border:0;" +
                "display:block;" +
                "pointer-events:none;" +
                "}" +

                "#touchLayer{" +
                "position:absolute;" +
                "left:0;" +
                "top:0;" +
                "width:100%;" +
                "height:100%;" +
                "z-index:500;" +
                "background:transparent;" +
                "touch-action:none;" +
                "}" +

                ".marker{" +
                "position:absolute;" +
                "width:30px;" +
                "height:30px;" +
                "margin-left:-15px;" +
                "margin-top:-30px;" +
                "font-size:30px;" +
                "line-height:30px;" +
                "text-align:center;" +
                "z-index:1000;" +
                "pointer-events:none;" +
                "text-shadow:1px 1px 2px white;" +
                "}" +

                "#zoom{" +
                "position:absolute;" +
                "right:12px;" +
                "top:12px;" +
                "z-index:2000;" +
                "background:white;" +
                "border-radius:5px;" +
                "box-shadow:0 1px 5px rgba(0,0,0,.4);" +
                "overflow:hidden;" +
                "}" +

                ".zoomButton{" +
                "width:48px;" +
                "height:48px;" +
                "border:0;" +
                "border-bottom:1px solid #ccc;" +
                "background:white;" +
                "font-size:27px;" +
                "font-weight:bold;" +
                "}" +

                ".zoomButton:last-child{" +
                "border-bottom:0;" +
                "}" +

                "#copyright{" +
                "position:absolute;" +
                "bottom:3px;" +
                "right:3px;" +
                "z-index:2000;" +
                "background:rgba(255,255,255,.8);" +
                "font-size:10px;" +
                "padding:2px 4px;" +
                "}" +

                "</style>" +

                "</head>" +

                "<body>" +

                "<div id='map'>" +

                "<div id='tiles'></div>" +

                "<div id='touchLayer'></div>" +

                "<div id='destination' " +
                "class='marker' " +
                "style='display:none;'>📍</div>" +

                "<div id='pickup' " +
                "class='marker' " +
                "style='display:none;'>🟢</div>" +

                "<div id='driver' " +
                "class='marker' " +
                "style='display:none;'>🛺</div>" +

                "<div id='zoom'>" +

                "<button class='zoomButton' " +
                "onclick='changeZoom(1)'>+</button>" +

                "<button class='zoomButton' " +
                "onclick='changeZoom(-1)'>−</button>" +

                "</div>" +

                "<div id='copyright'>" +
                "© OpenStreetMap contributors" +
                "</div>" +

                "</div>" +

                "<script>" +

                "var lat=" + startLat + ";" +
                "var lng=" + startLng + ";" +
                "var zoom=15;" +

                "var centerX=0;" +
                "var centerY=0;" +

                "var dragging=false;" +
                "var moved=false;" +
                "var downX=0;" +
                "var downY=0;" +

                "var destinationLat=null;" +
                "var destinationLng=null;" +
                "var pickupLat=null;" +
                "var pickupLng=null;" +
                "var driverLat=null;" +
                "var driverLng=null;" +

                "function tileX(lon,z){" +
                "return (lon+180)/360*Math.pow(2,z);" +
                "}" +

                "function tileY(la,z){" +
                "var r=la*Math.PI/180;" +
                "return (1-Math.asinh(Math.tan(r))/Math.PI)/2*Math.pow(2,z);" +
                "}" +

                "function lonFromX(x,z){" +
                "return x/Math.pow(2,z)*360-180;" +
                "}" +

                "function latFromY(y,z){" +
                "var n=Math.PI-2*Math.PI*y/" +
                "Math.pow(2,z);" +
                "return 180/Math.PI*" +
                "Math.atan(Math.sinh(n));" +
                "}" +

                "function render(){" +

                "var map=document.getElementById('map');" +
                "var tiles=document.getElementById('tiles');" +

                "tiles.innerHTML='';" +

                "var w=map.clientWidth;" +
                "var h=map.clientHeight;" +

                "centerX=" +
                "tileX(lng,zoom)*256;" +

                "centerY=" +
                "tileY(lat,zoom)*256;" +

                "var left=centerX-w/2;" +
                "var top=centerY-h/2;" +

                "var firstX=Math.floor(left/256)-1;" +
                "var lastX=Math.floor((left+w)/256)+1;" +

                "var firstY=Math.floor(top/256)-1;" +
                "var lastY=Math.floor((top+h)/256)+1;" +

                "for(var x=firstX;x<=lastX;x++){" +

                "for(var y=firstY;y<=lastY;y++){" +

                "var max=Math.pow(2,zoom);" +

                "var xx=((x%max)+max)%max;" +

                "if(y<0||y>=max)continue;" +

                "var img=document.createElement('img');" +

                "img.className='tile';" +
                "img.draggable=false;" +
                "img.src=" +
                "'https://tile.openstreetmap.org/'" +
                "+zoom+'/'+xx+'/'+y+'.png';" +

                "img.style.left=" +
                "(x*256-left)+'px';" +

                "img.style.top=" +
                "(y*256-top)+'px';" +

                "tiles.appendChild(img);" +

                "}" +
                "}" +

                "positionMarker(" +
                "'destination'," +
                "destinationLat," +
                "destinationLng);" +

                "positionMarker(" +
                "'pickup'," +
                "pickupLat," +
                "pickupLng);" +

                "positionMarker(" +
                "'driver'," +
                "driverLat," +
                "driverLng);" +

                "}" +

                "function positionMarker(id,la,lo){" +

                "if(la===null||lo===null)return;" +

                "var marker=" +
                "document.getElementById(id);" +

                "var map=" +
                "document.getElementById('map');" +

                "var left=" +
                "centerX-map.clientWidth/2;" +

                "var top=" +
                "centerY-map.clientHeight/2;" +

                "var px=" +
                "tileX(lo,zoom)*256-left;" +

                "var py=" +
                "tileY(la,zoom)*256-top;" +

                "marker.style.left=px+'px';" +
                "marker.style.top=py+'px';" +
                "marker.style.display='block';" +

                "}" +

                "function selectPoint(x,y){" +

                "var map=" +
                "document.getElementById('map');" +

                "var left=" +
                "centerX-map.clientWidth/2;" +

                "var top=" +
                "centerY-map.clientHeight/2;" +

                "var worldX=x+left;" +
                "var worldY=y+top;" +

                "var la=" +
                "latFromY(worldY/256,zoom);" +

                "var lo=" +
                "lonFromX(worldX/256,zoom);" +

                "destinationLat=la;" +
                "destinationLng=lo;" +

                "positionMarker(" +
                "'destination',la,lo);" +

                "Android.mapClicked(la,lo);" +

                "}" +

                "function changeZoom(amount){" +

                "zoom+=amount;" +

                "if(zoom<3)zoom=3;" +
                "if(zoom>19)zoom=19;" +

                "render();" +

                "}" +

                "function centerMap(la,lo){" +

                "lat=la;" +
                "lng=lo;" +

                "render();" +

                "}" +

                "function setDestination(la,lo){" +

                "destinationLat=la;" +
                "destinationLng=lo;" +

                "render();" +

                "}" +

                "function setPickup(la,lo){" +

                "pickupLat=la;" +
                "pickupLng=lo;" +

                "render();" +

                "}" +

                "function setDriver(la,lo){" +

                "driverLat=la;" +
                "driverLng=lo;" +

                "lat=la;" +
                "lng=lo;" +

                "render();" +

                "}" +

                "var touchLayer=" +
                "document.getElementById('touchLayer');" +

                "touchLayer.addEventListener(" +
                "'touchstart'," +

                "function(e){" +

                "if(e.touches.length!==1)return;" +

                "dragging=true;" +
                "moved=false;" +

                "downX=" +
                "e.touches[0].clientX;" +

                "downY=" +
                "e.touches[0].clientY;" +

                "e.preventDefault();" +

                "}," +

                "{passive:false});" +

                "touchLayer.addEventListener(" +
                "'touchmove'," +

                "function(e){" +

                "if(!dragging)return;" +

                "if(e.touches.length!==1)return;" +

                "var dx=" +
                "e.touches[0].clientX-downX;" +

                "var dy=" +
                "e.touches[0].clientY-downY;" +

                "if(Math.abs(dx)>8||Math.abs(dy)>8){" +
                "moved=true;" +
                "}" +

                "e.preventDefault();" +

                "}," +

                "{passive:false});" +

                "touchLayer.addEventListener(" +
                "'touchend'," +

                "function(e){" +

                "if(!dragging)return;" +

                "var touch=e.changedTouches[0];" +

                "if(!moved){" +

                "selectPoint(" +
                "touch.clientX," +
                "touch.clientY" +
                ");" +

                "}" +

                "dragging=false;" +

                "e.preventDefault();" +

                "}," +

                "{passive:false});" +

                "touchLayer.addEventListener(" +
                "'click'," +

                "function(e){" +

                "selectPoint(" +
                "e.clientX," +
                "e.clientY" +
                ");" +

                "});" +

                "window.onload=function(){" +

                "render();" +

                "setTimeout(function(){" +
                "render();" +
                "Android.mapReady();" +
                "},500);" +

                "};" +

                "</script>" +

                "</body>" +
                "</html>";

        webView.loadDataWithBaseURL(
                "https://www.openstreetmap.org/",
                html,
                "text/html",
                "UTF-8",
                null);
    }

    private double getInitialLatitude() {

        if (pickupLatitude != 0.0) {
            return pickupLatitude;
        }

        if (destinationLatitude != 0.0) {
            return destinationLatitude;
        }

        return 14.35;
    }

    private double getInitialLongitude() {

        if (pickupLongitude != 0.0) {
            return pickupLongitude;
        }

        if (destinationLongitude != 0.0) {
            return destinationLongitude;
        }

        return 121.05;
    }

    private class MapBridge {

        @JavascriptInterface
        public void mapReady() {

            runOnUiThread(() -> {

                mapReady = true;

                if (pickupLatitude != 0.0 &&
                        pickupLongitude != 0.0) {

                    webView.loadUrl(
                            "javascript:setPickup(" +
                                    pickupLatitude +
                                    "," +
                                    pickupLongitude +
                                    ")");
                }

                if ("SELECT_DESTINATION".equals(mode) &&
                        destinationLatitude != 0.0 &&
                        destinationLongitude != 0.0) {

                    webView.loadUrl(
                            "javascript:setDestination(" +
                                    destinationLatitude +
                                    "," +
                                    destinationLongitude +
                                    ")");
                }
            });
        }

        @JavascriptInterface
        public void mapClicked(
                double lat,
                double lng) {

            runOnUiThread(() -> {

                destinationLatitude = lat;
                destinationLongitude = lng;

                selectedText.setText(
                        String.format(
                                Locale.US,
                                "Selected: %.6f, %.6f\n" +
                                "Getting address...",
                                lat,
                                lng));

                reverseGeocode(lat, lng);
            });
        }

        @JavascriptInterface
        public void mapError(
                String message) {

            runOnUiThread(() -> {

                selectedText.setText(
                        "OpenStreetMap could not load.");

                Toast.makeText(
                        MapActivity.this,
                        "Map loading error.",
                        Toast.LENGTH_LONG).show();
            });
        }
    }

    private void reverseGeocode(
            double lat,
            double lng) {

        Executors.newSingleThreadExecutor()
                .execute(() -> {

                    String address =
                            getAddressText(lat, lng);

                    runOnUiThread(() -> {

                        if (address == null ||
                                address.trim().isEmpty()) {

                            destinationAddress =
                                    String.format(
                                            Locale.US,
                                            "%.6f, %.6f",
                                            lat,
                                            lng);

                        } else {

                            destinationAddress =
                                    address;
                        }

                        selectedText.setText(
                                "Destination:\n" +
                                destinationAddress);
                    });
                });
    }

    private String getAddressText(
            double lat,
            double lng) {

        if (!Geocoder.isPresent()) {
            return null;
        }

        try {

            Geocoder geocoder =
                    new Geocoder(
                            this,
                            Locale.getDefault());

            List<Address> addresses =
                    geocoder.getFromLocation(
                            lat,
                            lng,
                            1);

            if (addresses != null &&
                    !addresses.isEmpty()) {

                Address address =
                        addresses.get(0);

                String line =
                        address.getAddressLine(0);

                if (line != null &&
                        !line.isEmpty()) {

                    return line;
                }
            }

        } catch (Exception ignored) {
        }

        return null;
    }

    private void centerOnMyLocation() {

        if (currentLocation == null) {

            Toast.makeText(
                    this,
                    "Waiting for your GPS location.",
                    Toast.LENGTH_SHORT).show();

            return;
        }

        double lat =
                currentLocation.getLatitude();

        double lng =
                currentLocation.getLongitude();

        pickupLatitude = lat;
        pickupLongitude = lng;

        if (mapReady) {

            webView.loadUrl(
                    "javascript:centerMap(" +
                            lat +
                            "," +
                            lng +
                            ")");

            webView.loadUrl(
                    "javascript:setPickup(" +
                            lat +
                            "," +
                            lng +
                            ")");
        }
    }

    private void confirmDestination() {

        if (destinationLatitude == 0.0 &&
                destinationLongitude == 0.0) {

            Toast.makeText(
                    this,
                    "Tap the map to select a destination first.",
                    Toast.LENGTH_LONG).show();

            return;
        }

        Intent result =
                new Intent();

        result.putExtra(
                "destination_latitude",
                destinationLatitude);

        result.putExtra(
                "destination_longitude",
                destinationLongitude);

        result.putExtra(
                "destination_address",
                destinationAddress);

        result.putExtra(
                "pickup_latitude",
                pickupLatitude);

        result.putExtra(
                "pickup_longitude",
                pickupLongitude);

        result.putExtra(
                "pickup_address",
                getIntent().getStringExtra(
                        "pickup_address"));

        setResult(
                RESULT_OK,
                result);

        finish();
    }

    private void setupLocation() {

        locationManager =
                (LocationManager)
                        getSystemService(
                                LOCATION_SERVICE);

        if (locationManager == null) {
            return;
        }

        if (checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                &&
                checkSelfPermission(
                        Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_REQUEST);

            return;
        }

        try {

            Location last =
                    locationManager.getLastKnownLocation(
                            LocationManager.GPS_PROVIDER);

            if (last == null) {

                last =
                        locationManager.getLastKnownLocation(
                                LocationManager.NETWORK_PROVIDER);
            }

            if (last != null) {
                currentLocation = last;
            }

            if (locationManager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER)) {

                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        2000,
                        5,
                        locationListener);

            } else if (
                    locationManager.isProviderEnabled(
                            LocationManager.NETWORK_PROVIDER)) {

                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        2000,
                        5,
                        locationListener);
            }

        } catch (SecurityException ignored) {
        } catch (Exception ignored) {
        }
    }

    private final LocationListener locationListener =
            new LocationListener() {

        @Override
        public void onLocationChanged(
                @NonNull Location location) {

            currentLocation = location;

            if ("SELECT_DESTINATION".equals(mode)
                    && pickupLatitude == 0.0
                    && pickupLongitude == 0.0) {

                pickupLatitude =
                        location.getLatitude();

                pickupLongitude =
                        location.getLongitude();

                if (mapReady) {

                    webView.loadUrl(
                            "javascript:centerMap(" +
                                    pickupLatitude +
                                    "," +
                                    pickupLongitude +
                                    ")");

                    webView.loadUrl(
                            "javascript:setPickup(" +
                                    pickupLatitude +
                                    "," +
                                    pickupLongitude +
                                    ")");
                }
            }
        }
    };

    private void listenForRide() {

        if (rideId == null ||
                rideId.isEmpty()) {
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

                                    updateLiveRide(snapshot);
                                });
    }

    private void updateLiveRide(
            DocumentSnapshot ride) {

        Double driverLat =
                ride.getDouble(
                        "driverLatitude");

        Double driverLng =
                ride.getDouble(
                        "driverLongitude");

        if (driverLat == null) {

            driverLat =
                    ride.getDouble(
                            "driverLat");
        }

        if (driverLng == null) {

            driverLng =
                    ride.getDouble(
                            "driverLng");
        }

        if (driverLat == null ||
                driverLng == null) {
            return;
        }

        if (mapReady) {

            final double lat = driverLat;
            final double lng = driverLng;

            runOnUiThread(() -> {

                webView.loadUrl(
                        "javascript:setDriver(" +
                                lat +
                                "," +
                                lng +
                                ")");

                selectedText.setText(
                        String.format(
                                Locale.US,
                                "Driver location: %.6f, %.6f",
                                lat,
                                lng));
            });
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
                grantResults);

        if (requestCode == LOCATION_REQUEST) {

            boolean granted = false;

            for (int result : grantResults) {

                if (result ==
                        PackageManager.PERMISSION_GRANTED) {

                    granted = true;
                    break;
                }
            }

            if (granted) {
                setupLocation();
            }
        }
    }

    @Override
    protected void onDestroy() {

        if (rideListener != null) {
            rideListener.remove();
            rideListener = null;
        }

        if (locationManager != null) {

            try {

                locationManager.removeUpdates(
                        locationListener);

            } catch (SecurityException ignored) {
            } catch (Exception ignored) {
            }
        }

        if (webView != null) {

            webView.stopLoading();
            webView.destroy();
        }

        super.onDestroy();
    }
}
