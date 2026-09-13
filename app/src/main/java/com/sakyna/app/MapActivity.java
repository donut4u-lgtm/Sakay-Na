package com.sakyna.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class MapActivity extends AppCompatActivity {

    private static final int LOCATION_REQUEST = 5001;

    private MapView mapView;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(
                this,
                PreferenceManager.getDefaultSharedPreferences(this)
        );

        Configuration.getInstance().setUserAgentValue(
                getPackageName()
        );

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        status = new TextView(this);
        status.setText("Sakay Na Map\nGetting your location...");
        status.setTextSize(18);
        status.setTextColor(Color.DKGRAY);
        status.setGravity(Gravity.CENTER);
        status.setPadding(20, 20, 20, 20);

        root.addView(
                status,
                new LinearLayout.LayoutParams(
                        -1,
                        150
                )
        );

        mapView = new MapView(this);
        mapView.setMultiTouchControls(true);

        root.addView(
                mapView,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        setContentView(root);

        mapView.getController().setZoom(15.0);

        requestLocation();
    }

    private void requestLocation() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_REQUEST
            );

            status.setText(
                    "Sakay Na Map\nLocation permission required."
            );

            return;
        }

        showDefaultMap();
    }

    private void showDefaultMap() {

        /*
         * Philippines starting position.
         * This is only the initial map position.
         * GPS will be added next.
         */
        GeoPoint philippines =
                new GeoPoint(
                        14.5995,
                        120.9842
                );

        mapView.getController().setCenter(
                philippines
        );

        Marker marker =
                new Marker(mapView);

        marker.setPosition(
                philippines
        );

        marker.setTitle(
                "Sakay Na"
        );

        marker.setSnippet(
                "Map is working"
        );

        mapView.getOverlays().add(
                marker
        );

        status.setText(
                "Sakay Na Map\nOpenStreetMap is working."
        );

        mapView.invalidate();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] results) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                results
        );

        if (requestCode == LOCATION_REQUEST) {

            if (results.length > 0
                    && results[0]
                    == PackageManager.PERMISSION_GRANTED) {

                showDefaultMap();

            } else {

                status.setText(
                        "Sakay Na Map\nLocation permission denied."
                );
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {

        if (mapView != null) {
            mapView.onPause();
        }

        super.onPause();
    }
}
