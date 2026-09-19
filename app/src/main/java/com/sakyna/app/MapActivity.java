
package com.sakyna.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapActivity extends Activity {

    private EditText searchInput;
    private LinearLayout resultsContainer;
    private TextView statusText;

    private double pickupLat;
    private double pickupLng;

    private String mode = "SELECT_DESTINATION";
    private String rideId = "";

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    private FirebaseFirestore db;
    private ListenerRegistration rideListener;

    private final List<PlaceResult> results =
            new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();

        mode = getIntent().getStringExtra("mode");

        if (mode == null) {
            mode = "SELECT_DESTINATION";
        }

        pickupLat = getIntent().getDoubleExtra(
                "pickup_latitude",
                0
        );

        pickupLng = getIntent().getDoubleExtra(
                "pickup_longitude",
                0
        );

        rideId = getIntent().getStringExtra("ride_id");

        if (rideId == null) {
            rideId = "";
        }

        buildScreen();

        if ("LIVE_RIDE".equalsIgnoreCase(mode)) {
            loadLiveRide();
        }
    }

    private void buildScreen() {

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setBackgroundColor(
                Color.rgb(245, 248, 246)
        );

        TextView header =
                new TextView(this);

        header.setText(
                "🛺 SAKAY NA"
        );

        header.setTextSize(26);

        header.setTextColor(
                Color.WHITE
        );

        header.setGravity(
                Gravity.CENTER
        );

        header.setPadding(
                10,
                22,
                10,
                22
        );

        header.setBackgroundColor(
                Color.rgb(0, 125, 75)
        );

        root.addView(
                header,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        if ("LIVE_RIDE".equalsIgnoreCase(mode)) {

            buildLiveRideScreen(root);

        } else {

            buildDestinationSearchScreen(root);
        }

        setContentView(root);
    }

    private void buildDestinationSearchScreen(
            LinearLayout root
    ) {

        TextView title =
                new TextView(this);

        title.setText(
                "🎯 CHOOSE DESTINATION"
        );

        title.setTextSize(22);

        title.setTextColor(
                Color.rgb(0, 110, 70)
        );

        title.setGravity(
                Gravity.CENTER
        );

        title.setPadding(
                10,
                20,
                10,
                12
        );

        root.addView(title);

        searchInput =
                new EditText(this);

        searchInput.setHint(
                "Search place or establishment"
        );

        searchInput.setSingleLine(true);

        searchInput.setTextSize(17);

        searchInput.setPadding(
                18,
                14,
                18,
                14
        );

        searchInput.setBackgroundColor(
                Color.WHITE
        );

        root.addView(
                searchInput,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        Button searchButton =
                new Button(this);

        searchButton.setText(
                "🔎 SEARCH"
        );

        searchButton.setTextSize(17);

        searchButton.setTextColor(
                Color.WHITE
        );

        searchButton.setBackgroundColor(
                Color.rgb(0, 125, 75)
        );

        searchButton.setOnClickListener(
                v -> searchPlace()
        );

        root.addView(searchButton);

        statusText =
                new TextView(this);

        statusText.setText(
                "Search for a real place such as Jollibee or SM City Imus."
        );

        statusText.setTextSize(15);

        statusText.setTextColor(
                Color.DKGRAY
        );

        statusText.setGravity(
                Gravity.CENTER
        );

        statusText.setPadding(
                10,
                12,
                10,
                12
        );

        root.addView(statusText);

        ScrollView scroll =
                new ScrollView(this);

        resultsContainer =
                new LinearLayout(this);

        resultsContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        resultsContainer.setPadding(
                12,
                5,
                12,
                25
        );

        scroll.addView(
                resultsContainer
        );

        root.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        Button cancel =
                new Button(this);

        cancel.setText(
                "✖ CANCEL"
        );

        cancel.setOnClickListener(
                v -> finish()
        );

        root.addView(cancel);
    }

    private void buildLiveRideScreen(
            LinearLayout root
    ) {

        TextView title =
                new TextView(this);

        title.setText(
                "🚕 LIVE RIDE"
        );

        title.setTextSize(24);

        title.setTextColor(
                Color.rgb(0, 110, 70)
        );

        title.setGravity(
                Gravity.CENTER
        );

        title.setPadding(
                10,
                25,
                10,
                20
        );

        root.addView(title);

        statusText =
                new TextView(this);

        statusText.setText(
                "Loading ride..."
        );

        statusText.setTextSize(19);

        statusText.setTextColor(
                Color.DKGRAY
        );

        statusText.setGravity(
                Gravity.CENTER
        );

        statusText.setPadding(
                20,
                30,
                20,
                30
        );

        root.addView(
                statusText,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        Button close =
                new Button(this);

        close.setText(
                "← BACK"
        );

        close.setOnClickListener(
                v -> finish()
        );

        root.addView(close);
    }

    private void searchPlace() {

        String query =
                searchInput
                        .getText()
                        .toString()
                        .trim();

        if (query.isEmpty()) {

            Toast.makeText(
                    this,
                    "Enter a place or establishment.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        statusText.setText(
                "🔎 Searching real places..."
        );

        resultsContainer.removeAllViews();

        executor.execute(() -> {

            List<PlaceResult> found =
                    searchWithPhoton(query);

            if (found.isEmpty()) {
                found = searchWithNominatim(query);
            }

            List<PlaceResult> finalResults =
                    found;

            mainHandler.post(() ->
                    showResults(finalResults)
            );
        });
    }

    /*
     * Photon is used first for establishment/place
     * search because it normally returns useful POI
     * coordinates and names.
     */
    private List<PlaceResult> searchWithPhoton(
            String query
    ) {

        List<PlaceResult> list =
                new ArrayList<>();

        try {

            String encoded =
                    URLEncoder.encode(
                            query,
                            "UTF-8"
                    );

            String url =
                    "https://photon.komoot.io/api/"
                    + "?q="
                    + encoded
                    + "&limit=10"
                    + "&lang=en";

            String json =
                    httpGet(url);

            if (json.isEmpty()) {
                return list;
            }

            JSONObject root =
                    new JSONObject(json);

            JSONArray features =
                    root.optJSONArray(
                            "features"
                    );

            if (features == null) {
                return list;
            }

            for (int i = 0;
                 i < features.length();
                 i++) {

                JSONObject feature =
                        features.getJSONObject(i);

                JSONObject geometry =
                        feature.optJSONObject(
                                "geometry"
                        );

                JSONObject properties =
                        feature.optJSONObject(
                                "properties"
                        );

                if (geometry == null
                        || properties == null) {
                    continue;
                }

                JSONArray coordinates =
                        geometry.optJSONArray(
                                "coordinates"
                        );

                if (coordinates == null
                        || coordinates.length() < 2) {
                    continue;
                }

                double lng =
                        coordinates.getDouble(0);

                double lat =
                        coordinates.getDouble(1);

                String name =
                        firstNonEmpty(
                                properties.optString(
                                        "name"
                                ),
                                properties.optString(
                                        "street"
                                ),
                                "Selected location"
                        );

                String address =
                        buildPhotonAddress(
                                properties
                        );

                String country =
                        properties.optString(
                                "country"
                        );

                if (!country.isEmpty()
                        && !country
                        .toLowerCase(Locale.US)
                        .contains("philippines")) {

                    continue;
                }

                list.add(
                        new PlaceResult(
                                lat,
                                lng,
                                name,
                                address
                        )
                );
            }

        } catch (Exception ignored) {
        }

        return list;
    }

    private List<PlaceResult> searchWithNominatim(
            String query
    ) {

        List<PlaceResult> list =
                new ArrayList<>();

        try {

            String encoded =
                    URLEncoder.encode(
                            query
                                    + ", Philippines",
                            "UTF-8"
                    );

            String url =
                    "https://nominatim.openstreetmap.org/search"
                    + "?format=jsonv2"
                    + "&q="
                    + encoded
                    + "&limit=10"
                    + "&addressdetails=1"
                    + "&namedetails=1"
                    + "&accept-language=en";

            String json =
                    httpGet(url);

            if (json.isEmpty()) {
                return list;
            }

            JSONArray array =
                    new JSONArray(json);

            for (int i = 0;
                 i < array.length();
                 i++) {

                JSONObject object =
                        array.getJSONObject(i);

                double lat =
                        Double.parseDouble(
                                object.getString("lat")
                        );

                double lng =
                        Double.parseDouble(
                                object.getString("lon")
                        );

                JSONObject namedetails =
                        object.optJSONObject(
                                "namedetails"
                        );

                JSONObject addressObject =
                        object.optJSONObject(
                                "address"
                        );

                String name =
                        getNominatimName(
                                object,
                                namedetails,
                                addressObject
                        );

                String address =
                        getNominatimAddress(
                                object,
                                addressObject,
                                name
                        );

                list.add(
                        new PlaceResult(
                                lat,
                                lng,
                                name,
                                address
                        )
                );
            }

        } catch (Exception ignored) {
        }

        return list;
    }

    private String getNominatimName(
            JSONObject object,
            JSONObject namedetails,
            JSONObject address
    ) {

        if (namedetails != null) {

            String brand =
                    namedetails.optString(
                            "brand"
                    );

            if (!brand.isEmpty()) {
                return brand;
            }

            String official =
                    namedetails.optString(
                            "official_name"
                    );

            if (!official.isEmpty()) {
                return official;
            }

            String name =
                    namedetails.optString(
                            "name"
                    );

            if (!name.isEmpty()) {
                return name;
            }
        }

        String name =
                object.optString("name");

        if (!name.isEmpty()) {
            return name;
        }

        if (address != null) {

            String amenity =
                    address.optString(
                            "amenity"
                    );

            if (!amenity.isEmpty()) {
                return amenity;
            }

            String shop =
                    address.optString(
                            "shop"
                    );

            if (!shop.isEmpty()) {
                return shop;
            }

            String tourism =
                    address.optString(
                            "tourism"
                    );

            if (!tourism.isEmpty()) {
                return tourism;
            }
        }

        return "Selected location";
    }

    private String getNominatimAddress(
            JSONObject object,
            JSONObject address,
            String name
    ) {

        if (address == null) {
            return object.optString(
                    "display_name",
                    name
            );
        }

        StringBuilder result =
                new StringBuilder();

        addPart(
                result,
                address.optString(
                        "house_number"
                )
        );

        addPart(
                result,
                address.optString(
                        "road"
                )
        );

        addPart(
                result,
                address.optString(
                        "suburb"
                )
        );

        addPart(
                result,
                address.optString(
                        "barangay"
                )
        );

        addPart(
                result,
                address.optString(
                        "city"
                )
        );

        addPart(
                result,
                address.optString(
                        "town"
                )
        );

        addPart(
                result,
                address.optString(
                        "municipality"
                )
        );

        addPart(
                result,
                address.optString(
                        "state"
                )
        );

        if (result.length() == 0) {

            return object.optString(
                    "display_name",
                    name
            );
        }

        return result.toString();
    }

    private String buildPhotonAddress(
            JSONObject properties
    ) {

        StringBuilder address =
                new StringBuilder();

        addPart(
                address,
                properties.optString(
                        "housenumber"
                )
        );

        addPart(
                address,
                properties.optString(
                        "street"
                )
        );

        addPart(
                address,
                properties.optString(
                        "district"
                )
        );

        addPart(
                address,
                properties.optString(
                        "city"
                )
        );

        addPart(
                address,
                properties.optString(
                        "state"
                )
        );

        return address.length() == 0
                ? "Philippines"
                : address.toString();
    }

    private void addPart(
            StringBuilder builder,
            String value
    ) {

        if (value == null
                || value.trim().isEmpty()) {
            return;
        }

        if (builder.length() > 0) {
            builder.append(", ");
        }

        builder.append(value.trim());
    }

    private String firstNonEmpty(
            String... values
    ) {

        for (String value : values) {

            if (value != null
                    && !value.trim().isEmpty()) {

                return value.trim();
            }
        }

        return "";
    }

    private void showResults(
            List<PlaceResult> found
    ) {

        results.clear();
        results.addAll(found);

        resultsContainer.removeAllViews();

        if (found.isEmpty()) {

            statusText.setText(
                    "🔴 No place found. Try the establishment name plus city."
            );

            return;
        }

        statusText.setText(
                "📍 SELECT THE CORRECT PLACE"
        );

        for (PlaceResult place : found) {

            addResultCard(place);
        }
    }

    private void addResultCard(
            PlaceResult place
    ) {

        LinearLayout card =
                new LinearLayout(this);

        card.setOrientation(
                LinearLayout.VERTICAL
        );

        card.setPadding(
                18,
                18,
                18,
                18
        );

        card.setBackgroundColor(
                Color.WHITE
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        params.setMargins(
                0,
                8,
                0,
                12
        );

        card.setLayoutParams(params);

        TextView name =
                new TextView(this);

        name.setText(
                "📍 " + place.name
        );

        name.setTextSize(19);

        name.setTextColor(
                Color.rgb(0, 110, 70)
        );

        name.setGravity(
                Gravity.CENTER_VERTICAL
        );

        name.setPadding(
                0,
                0,
                0,
                8
        );

        card.addView(name);

        TextView address =
                new TextView(this);

        address.setText(
                place.address
                        + "\n\n"
                        + String.format(
                                Locale.US,
                                "Coordinates: %.6f, %.6f",
                                place.latitude,
                                place.longitude
                        )
        );

        address.setTextSize(15);

        address.setTextColor(
                Color.DKGRAY
        );

        card.addView(address);

        Button select =
                new Button(this);

        select.setText(
                "✅ USE THIS DESTINATION"
        );

        select.setTextColor(
                Color.WHITE
        );

        select.setBackgroundColor(
                Color.rgb(0, 150, 80)
        );

        select.setOnClickListener(
                v -> selectDestination(place)
        );

        card.addView(select);

        resultsContainer.addView(card);
    }

    private void selectDestination(
            PlaceResult place
    ) {

        android.content.Intent result =
                new android.content.Intent();

        result.putExtra(
                "destination_latitude",
                place.latitude
        );

        result.putExtra(
                "destination_longitude",
                place.longitude
        );

        result.putExtra(
                "destinationName",
                place.name
        );

        result.putExtra(
                "destination_address",
                place.address
        );

        result.putExtra(
                "latitude",
                place.latitude
        );

        result.putExtra(
                "longitude",
                place.longitude
        );

        setResult(
                RESULT_OK,
                result
        );

        finish();
    }

    private void loadLiveRide() {

        if (rideId.isEmpty()) {

            statusText.setText(
                    "No active ride."
            );

            return;
        }

        rideListener =
                db.collection("rides")
                        .document(rideId)
                        .addSnapshotListener(
                                (snapshot, error) -> {

                                    if (error != null) {

                                        statusText.setText(
                                                "🔴 Unable to load ride."
                                        );

                                        return;
                                    }

                                    if (snapshot == null
                                            || !snapshot.exists()) {

                                        statusText.setText(
                                                "Ride no longer exists."
                                        );

                                        return;
                                    }

                                    showLiveRide(
                                            snapshot
                                    );
                                }
                        );
    }

    private void showLiveRide(
            DocumentSnapshot ride
    ) {

        String pickup =
                safe(
                        ride.getString(
                                "pickupName"
                        ),
                        ride.getString(
                                "pickup"
                        )
                );

        String destination =
                safe(
                        ride.getString(
                                "destinationName"
                        ),
                        ride.getString(
                                "destination"
                        )
                );

        String status =
                safe(
                        ride.getString(
                                "status"
                        ),
                        "UNKNOWN"
                );

        String driverId =
                safe(
                        ride.getString(
                                "driverId"
                        ),
                        "Not assigned"
                );

        statusText.setText(
                "📍 PICKUP\n"
                        + pickup
                        + "\n\n"
                        + "🎯 DESTINATION\n"
                        + destination
                        + "\n\n"
                        + "🚦 STATUS\n"
                        + status
                        + "\n\n"
                        + "🚕 DRIVER\n"
                        + driverId
                        + "\n\n"
                        + "Map view is currently unavailable."
        );
    }

    private String safe(
            String first,
            String second
    ) {

        if (first != null
                && !first.trim().isEmpty()) {
            return first;
        }

        if (second != null
                && !second.trim().isEmpty()) {
            return second;
        }

        return "Not available";
    }

    private String httpGet(
            String urlString
    ) {

        HttpURLConnection connection =
                null;

        try {

            URL url =
                    new URL(urlString);

            connection =
                    (HttpURLConnection)
                            url.openConnection();

            connection.setRequestMethod(
                    "GET"
            );

            connection.setConnectTimeout(
                    10000
            );

            connection.setReadTimeout(
                    15000
            );

            connection.setRequestProperty(
                    "User-Agent",
                    "SakayNa/1.0 Android"
            );

            InputStream input =
                    connection.getInputStream();

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    input
                            )
                    );

            StringBuilder result =
                    new StringBuilder();

            String line;

            while (
                    (line = reader.readLine())
                            != null
            ) {

                result.append(line);
            }

            reader.close();

            return result.toString();

        } catch (Exception ignored) {

            return "";

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Override
    protected void onDestroy() {

        if (rideListener != null) {
            rideListener.remove();
            rideListener = null;
        }

        executor.shutdownNow();

        super.onDestroy();
    }

    private static class PlaceResult {

        final double latitude;
        final double longitude;
        final String name;
        final String address;

        PlaceResult(
                double latitude,
                double longitude,
                String name,
                String address
        ) {

            this.latitude = latitude;
            this.longitude = longitude;
            this.name = name;
            this.address = address;
        }
    }
}
