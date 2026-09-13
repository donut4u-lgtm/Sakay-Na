
package com.sakyna.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.osmdroid.config.Configuration;
import org.osmdroid.library.R;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

public class MapActivity extends AppCompatActivity {

    private static final int LOCATION_REQUEST = 5001;

    private MapView mapView;
    private TextView status;
    private MyLocationNewOverlay myLocationOverlay;

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
        status.setText("Sakay Na Map\nGetting GPS location...");
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

        startLocation();
    }

    private void startLocation() {

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
                    "Sakay Na Map\nPlease allow location."
            );

            return;
        }

        myLocationOverlay =
                new MyLocationNewOverlay(
                        new GpsMyLocationProvider(this),
                        mapView
                );

        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();

        myLocationOverlay.runOnFirstFix(
                () -> runOnUiThread(() -> {

                    GeoPoint location =
                            myLocationOverlay.getMyLocation();

                    if (location != null) {

                        mapView.getController()
                                .animateTo(location);

                        status.setText(
                                "Sakay Na Map\nGPS location found."
                        );
                    }
                })
        );

        mapView.getOverlays().add(
                myLocationOverlay
        );

        status.setText(
                "Sakay Na Map\nWaiting for GPS..."
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

                startLocation();

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
