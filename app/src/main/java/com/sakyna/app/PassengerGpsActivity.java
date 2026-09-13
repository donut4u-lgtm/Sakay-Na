

package com.sakyna.app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PassengerGpsActivity extends Activity {

    private static final int LOCATION_REQUEST = 5001;

    private LocationManager locationManager;
    private LocationListener locationListener;

    private TextView pickupText;
    private TextView destinationText;
    private TextView distanceText;

    private double pickupLatitude;
    private double pickupLongitude;

    private double destinationLatitude;
    private double destinationLongitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 40, 30, 30);

        TextView title = new TextView(this);
        title.setText("SAKAY NA\nRIDE LOCATION");
        title.setTextSize(24);
        layout.addView(title);

        pickupText = new TextView(this);
        pickupText.setTextSize(18);
        pickupText.setText(
                "\nPICKUP\n\nWaiting for GPS..."
        );
        layout.addView(pickupText);

        Button setPickupButton = new Button(this);
        setPickupButton.setText("SET CURRENT LOCATION AS PICKUP");
        layout.addView(setPickupButton);

        destinationText = new TextView(this);
        destinationText.setTextSize(18);
        destinationText.setText(
                "\nDESTINATION\n\nNot set"
        );
        layout.addView(destinationText);

        EditText latitudeInput = new EditText(this);
        latitudeInput.setHint("Destination Latitude");
        latitudeInput.setInputType(8194);
        layout.addView(latitudeInput);

        EditText longitudeInput = new EditText(this);
        longitudeInput.setHint("Destination Longitude");
        longitudeInput.setInputType(8194);
        layout.addView(longitudeInput);

        Button setDestinationButton = new Button(this);
        setDestinationButton.setText("SET DESTINATION");
        layout.addView(setDestinationButton);

        distanceText = new TextView(this);
        distanceText.setTextSize(18);
        distanceText.setText(
                "\nDISTANCE\n\nWaiting..."
        );
        layout.addView(distanceText);

        setPickupButton.setOnClickListener(v -> {

            if (pickupLatitude == 0.0
                    && pickupLongitude == 0.0) {

                pickupText.setText(
                        "\nPICKUP\n\nGPS location not ready."
                );

                return;
            }

            pickupText.setText(
                    "\nPICKUP\n\n" +
                    "Latitude: " + pickupLatitude + "\n" +
                    "Longitude: " + pickupLongitude
            );
        });

        setDestinationButton.setOnClickListener(v -> {

            try {

                destinationLatitude =
                        Double.parseDouble(
                                latitudeInput.getText()
                                        .toString()
                                        .trim()
                        );

                destinationLongitude =
                        Double.parseDouble(
                                longitudeInput.getText()
                                        .toString()
                                        .trim()
                        );

                destinationText.setText(
                        "\nDESTINATION\n\n" +
                        "Latitude: " + destinationLatitude + "\n" +
                        "Longitude: " + destinationLongitude
                );

                calculateDistance();

            } catch (Exception e) {

                destinationText.setText(
                        "\nDESTINATION\n\n" +
                        "Enter valid coordinates."
                );
            }
        });

        setContentView(layout);

        startGps();
    }

    private void startGps() {

        if (checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(
                Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_REQUEST
            );

            return;
        }

        locationManager =
                (LocationManager) getSystemService(
                        Context.LOCATION_SERVICE
                );

        locationListener = new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {

                pickupLatitude =
                        location.getLatitude();

                pickupLongitude =
                        location.getLongitude();

                pickupText.setText(
                        "\nCURRENT GPS\n\n" +
                        "Latitude: " + pickupLatitude + "\n" +
                        "Longitude: " + pickupLongitude
                );
            }
        };

        try {

            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000,
                    1,
                    locationListener
            );

        } catch (SecurityException ignored) {
        }
    }

    private void calculateDistance() {

        if (pickupLatitude == 0.0
                && pickupLongitude == 0.0) {

            distanceText.setText(
                    "\nDISTANCE\n\nWaiting for pickup GPS..."
            );

            return;
        }

        float[] result = new float[1];

        Location.distanceBetween(
                pickupLatitude,
                pickupLongitude,
                destinationLatitude,
                destinationLongitude,
                result
        );

        double kilometers =
                result[0] / 1000.0;

        distanceText.setText(
                String.format(
                        "\nDISTANCE\n\n%.2f km",
                        kilometers
                )
        );
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == LOCATION_REQUEST
                && grantResults.length > 0
                && grantResults[0]
                == PackageManager.PERMISSION_GRANTED) {

            startGps();
        }
    }

    @Override
    protected void onDestroy() {

        if (locationManager != null
                && locationListener != null) {

            locationManager.removeUpdates(
                    locationListener
            );
        }

        super.onDestroy();
    }
}
