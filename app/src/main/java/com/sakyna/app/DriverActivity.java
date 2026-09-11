

package com.sakyna.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class DriverActivity extends Activity {

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(
                "SakayNa",
                MODE_PRIVATE
        );

        showDashboard();
    }

    private TextView makeText(
            String text,
            int size
    ) {

        TextView view =
                new TextView(this);

        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(Color.DKGRAY);

        view.setPadding(
                20,
                20,
                20,
                20
        );

        return view;
    }

    private Button makeButton(
            String text
    ) {

        Button button =
                new Button(this);

        button.setText(text);
        button.setTextSize(17);
        button.setAllCaps(false);

        return button;
    }

    private LinearLayout createPage(
            String titleText
    ) {

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                25,
                25,
                25,
                25
        );

        TextView title =
                makeText(
                        titleText,
                        28
                );

        title.setTypeface(
                null,
                Typeface.BOLD
        );

        title.setTextColor(
                Color.rgb(
                        30,
                        100,
                        200
                )
        );

        title.setGravity(
                Gravity.CENTER
        );

        layout.addView(title);

        return layout;
    }

    private void addBackButton(
            LinearLayout layout
    ) {

        Button back =
                makeButton(
                        "Back to Dashboard"
                );

        back.setOnClickListener(
                v -> showDashboard()
        );

        layout.addView(back);
    }

    private void showDashboard() {

        LinearLayout layout =
                createPage(
                        "SAKAY NA"
                );

        TextView title =
                makeText(
                        "Driver Dashboard",
                        21
                );

        title.setGravity(
                Gravity.CENTER
        );

        layout.addView(title);

        boolean online =
                prefs.getBoolean(
                        "driver_online",
                        false
                );

        TextView status =
                makeText(
                        online
                        ? "● DRIVER IS ONLINE"
                        : "● DRIVER IS OFFLINE",
                        21
                );

        status.setGravity(
                Gravity.CENTER
        );

        status.setTypeface(
                null,
                Typeface.BOLD
        );

        status.setTextColor(
                online
                ? Color.rgb(
                        0,
                        150,
                        80
                )
                : Color.rgb(
                        180,
                        80,
                        80
                )
        );

        layout.addView(status);

        Button onlineButton;

        if (online) {

            onlineButton =
                    makeButton(
                            "🔴 Go Offline"
                    );

        } else {

            onlineButton =
                    makeButton(
                            "🟢 Go Online"
                    );
        }

        onlineButton.setOnClickListener(
                v -> toggleOnline()
        );

        layout.addView(onlineButton);

        Button requests =
                makeButton(
                        "📥 Booking Requests"
                );

        requests.setOnClickListener(
                v -> showBookingRequest()
        );

        layout.addView(requests);

        Button current =
                makeButton(
                        "🚕 Current Trip"
                );

        current.setOnClickListener(
                v -> showCurrentTrip()
        );

        layout.addView(current);

        Button earnings =
                makeButton(
                        "💰 Earnings & History"
                );

        earnings.setOnClickListener(
                v -> showEarnings()
        );

        layout.addView(earnings);

        Button logout =
                makeButton(
                        "Logout"
                );

        logout.setOnClickListener(
                v -> logout()
        );

        layout.addView(logout);

        setContentView(layout);
    }

    private void toggleOnline() {

        boolean online =
                prefs.getBoolean(
                        "driver_online",
                        false
                );

        prefs.edit()
                .putBoolean(
                        "driver_online",
                        !online
                )
                .apply();

        Toast.makeText(
                this,
                online
                ? "You are now OFFLINE."
                : "You are now ONLINE.",
                Toast.LENGTH_LONG
        ).show();

        showDashboard();
    }

    private void showBookingRequest() {

        LinearLayout layout =
                createPage(
                        "BOOKING REQUEST"
                );

        boolean online =
                prefs.getBoolean(
                        "driver_online",
                        false
                );

        String pickup =
                prefs.getString(
                        "ride_pickup",
                        ""
                );

        String destination =
                prefs.getString(
                        "ride_destination",
                        ""
                );

        String status =
                prefs.getString(
                        "ride_status",
                        ""
                );

        int fare =
                prefs.getInt(
                        "ride_fare",
                        0
                );

        if (!online) {

            TextView offline =
                    makeText(
                            "You are offline.\n\nGo ONLINE to receive booking requests.",
                            20
                    );

            offline.setGravity(
                    Gravity.CENTER
            );

            layout.addView(offline);

            addBackButton(layout);

            setContentView(layout);

            return;
        }

        if (pickup.isEmpty() ||
                destination.isEmpty() ||
                !status.equals(
                        "REQUESTED"
                )) {

            TextView none =
                    makeText(
                            "No new booking requests.",
                            20
                    );

            none.setGravity(
                    Gravity.CENTER
            );

            layout.addView(none);

            addBackButton(layout);

            setContentView(layout);

            return;
        }

        TextView request =
                makeText(
                        "NEW RIDE REQUEST\n\n" +
                        "Pickup:\n" +
                        pickup +
                        "\n\n" +
                        "Destination:\n" +
                        destination +
                        "\n\n" +
                        "Estimated Fare:\n" +
                        "₱" +
                        fare,
                        19
                );

        request.setTypeface(
                null,
                Typeface.BOLD
        );

        layout.addView(request);

        Button accept =
                makeButton(
                        "✅ ACCEPT RIDE"
                );

        accept.setOnClickListener(
                v -> acceptRide()
        );

        layout.addView(accept);

        Button decline =
                makeButton(
                        "❌ DECLINE RIDE"
                );

        decline.setOnClickListener(
                v -> declineRide()
        );

        layout.addView(decline);

        addBackButton(layout);

        setContentView(layout);
    }

    private void acceptRide() {

        String driverName =
                prefs.getString(
                        "current_name",
                        "Sakay Na Driver"
                );

        prefs.edit()
                .putString(
                        "ride_status",
                        "ACCEPTED"
                )
                .putString(
                        "ride_driver",
                        driverName
                )
                .apply();

        Toast.makeText(
                this,
                "Ride accepted!",
                Toast.LENGTH_LONG
        ).show();

        showCurrentTrip();
    }

    private void declineRide() {

        prefs.edit()
                .putString(
                        "ride_status",
                        "DECLINED"
                )
                .apply();

        Toast.makeText(
                this,
                "Ride declined.",
                Toast.LENGTH_LONG
        ).show();

        showBookingRequest();
    }

    private void showCurrentTrip() {

        LinearLayout layout =
                createPage(
                        "CURRENT TRIP"
                );

        String pickup =
                prefs.getString(
                        "ride_pickup",
                        ""
                );

        String destination =
                prefs.getString(
                        "ride_destination",
                        ""
                );

        String status =
                prefs.getString(
                        "ride_status",
                        ""
                );

        String passenger =
                prefs.getString(
                        "current_passenger",
                        "Passenger"
                );

        String driver =
                prefs.getString(
                        "ride_driver",
                        ""
                );

        int fare =
                prefs.getInt(
                        "ride_fare",
                        0
                );

        if (pickup.isEmpty() ||
                destination.isEmpty() ||
                status.isEmpty()) {

            TextView none =
                    makeText(
                            "No current trip.",
                            20
                    );

            none.setGravity(
                    Gravity.CENTER
            );

            layout.addView(none);

            addBackButton(layout);

            setContentView(layout);

            return;
        }

        TextView trip =
                makeText(
                        "PASSENGER\n" +
                        passenger +
                        "\n\n" +
                        "PICKUP\n" +
                        pickup +
                        "\n\n" +
                        "DESTINATION\n" +
                        destination +
                        "\n\n" +
                        "FARE\n₱" +
                        fare +
                        "\n\n" +
                        "DRIVER\n" +
                        (
                            driver.isEmpty()
                            ? "Not assigned"
                            : driver
                        ) +
                        "\n\n" +
                        "STATUS\n" +
                        getReadableStatus(
                                status
                        ),
                        18
                );

        trip.setTypeface(
                null,
                Typeface.BOLD
        );

        layout.addView(trip);

        TextView statusText =
                makeText(
                        getReadableStatus(
                                status
                        ),
                        22
                );

        statusText.setGravity(
                Gravity.CENTER
        );

        statusText.setTypeface(
                null,
                Typeface.BOLD
        );

        statusText.setTextColor(
                getStatusColor(
                        status
                )
        );

        layout.addView(statusText);

        addTripAction(
                layout,
                status
        );

        Button refresh =
                makeButton(
                        "🔄 Refresh Status"
                );

        refresh.setOnClickListener(
                v -> showCurrentTrip()
        );

        layout.addView(refresh);

        addBackButton(layout);

        setContentView(layout);
    }

    private void addTripAction(
            LinearLayout layout,
            String status
    ) {

        if (status.equals(
                "ACCEPTED"
        )) {

            Button button =
                    makeButton(
                            "🛣️ DRIVER ON THE WAY"
                    );

            button.setOnClickListener(
                    v -> updateTrip(
                            "DRIVER_ON_THE_WAY"
                    )
            );

            layout.addView(button);

            return;
        }

        if (status.equals(
                "DRIVER_ON_THE_WAY"
        )) {

            Button button =
                    makeButton(
                            "📍 ARRIVED"
                    );

            button.setOnClickListener(
                    v -> updateTrip(
                            "DRIVER_ARRIVED"
                    )
            );

            layout.addView(button);

            return;
        }

        if (status.equals(
                "DRIVER_ARRIVED"
        )) {

            Button button =
                    makeButton(
                            "▶️ START TRIP"
                    );

            button.setOnClickListener(
                    v -> updateTrip(
                            "IN_PROGRESS"
                    )
            );

            layout.addView(button);

            return;
        }

        if (status.equals(
                "IN_PROGRESS"
        )) {

            Button button =
                    makeButton(
                            "🏁 FINISH TRIP"
                    );

            button.setOnClickListener(
                    v -> finishTrip()
            );

            layout.addView(button);
        }
    }

    private void updateTrip(
            String newStatus
    ) {

        prefs.edit()
                .putString(
                        "ride_status",
                        newStatus
                )
                .apply();

        Toast.makeText(
                this,
                getReadableStatus(
                        newStatus
                ),
                Toast.LENGTH_LONG
        ).show();

        showCurrentTrip();
    }

    private void finishTrip() {

        int fare =
                prefs.getInt(
                        "ride_fare",
                        50
                );

        prefs.edit()
                .putString(
                        "ride_status",
                        "COMPLETED"
                )
                .putInt(
                        "ride_final_fare",
                        fare
                )
                .apply();

        Toast.makeText(
                this,
                "Trip finished successfully!\nFare: ₱" +
                fare,
                Toast.LENGTH_LONG
        ).show();

        showCompletedTrip();
    }

    private void showCompletedTrip() {

        LinearLayout layout =
                createPage(
                        "TRIP COMPLETED"
                );

        String pickup =
                prefs.getString(
                        "ride_pickup",
                        "Unknown"
                );

        String destination =
                prefs.getString(
                        "ride_destination",
                        "Unknown"
                );

        String driver =
                prefs.getString(
                        "ride_driver",
                        "Sakay Na Driver"
                );

        int fare =
                prefs.getInt(
                        "ride_final_fare",
                        prefs.getInt(
                                "ride_fare",
                                0
                        )
                );

        String rating =
                prefs.getString(
                        "ride_rating",
                        ""
                );

        TextView completed =
                makeText(
                        "✅ RIDE COMPLETED\n\n" +
                        "Pickup:\n" +
                        pickup +
                        "\n\n" +
                        "Destination:\n" +
                        destination +
                        "\n\n" +
                        "Driver:\n" +
                        driver +
                        "\n\n" +
                        "FINAL FARE\n" +
                        "₱" +
                        fare +
                        "\n\n" +
                        (
                            rating.isEmpty()
                            ? "Passenger Rating: Not yet rated"
                            : "Passenger Rating: " +
                              rating +
                              " / 5"
                        ),
                        19
                );

        completed.setGravity(
                Gravity.CENTER
        );

        completed.setTypeface(
                null,
                Typeface.BOLD
        );

        completed.setTextColor(
                Color.rgb(
                        0,
                        130,
                        70
                )
        );

        layout.addView(completed);

        Button receipt =
                makeButton(
                        "🧾 View Trip Receipt"
                );

        receipt.setOnClickListener(
                v -> showDriverReceipt()
        );

        layout.addView(receipt);

        addBackButton(layout);

        setContentView(layout);
    }

    private void showDriverReceipt() {

        LinearLayout layout =
                createPage(
                        "TRIP RECEIPT"
                );

        String pickup =
                prefs.getString(
                        "ride_pickup",
                        "Unknown"
                );

        String destination =
                prefs.getString(
                        "ride_destination",
                        "Unknown"
                );

        String driver =
                prefs.getString(
                        "ride_driver",
                        "Sakay Na Driver"
                );

        int fare =
                prefs.getInt(
                        "ride_final_fare",
                        prefs.getInt(
                                "ride_fare",
                                0
                        )
                );

        String rating =
                prefs.getString(
                        "ride_rating",
                        ""
                );

        TextView receipt =
                makeText(
                        "SAKAY NA\n\n" +
                        "COMPLETED TRIP\n\n" +
                        "Pickup:\n" +
                        pickup +
                        "\n\n" +
                        "Destination:\n" +
                        destination +
                        "\n\n" +
                        "Driver:\n" +
                        driver +
                        "\n\n" +
                        "Final Fare:\n" +
                        "₱" +
                        fare +
                        "\n\n" +
                        "Driver Earnings:\n" +
                        "₱" +
                        fare +
                        "\n\n" +
                        (
                            rating.isEmpty()
                            ? "Passenger Rating: Not yet rated"
                            : "Passenger Rating: " +
                              rating +
                              " / 5"
                        ),
                        19
                );

        receipt.setGravity(
                Gravity.CENTER
        );

        receipt.setTypeface(
                null,
                Typeface.BOLD
        );

        layout.addView(receipt);

        addBackButton(layout);

        setContentView(layout);
    }

    private void showEarnings() {

        LinearLayout layout =
                createPage(
                        "EARNINGS & HISTORY"
                );

        String status =
                prefs.getString(
                        "ride_status",
                        ""
                );

        int fare =
                prefs.getInt(
                        "ride_final_fare",
                        prefs.getInt(
                                "ride_fare",
                                0
                        )
                );

        String rating =
                prefs.getString(
                        "ride_rating",
                        ""
                );

        if (!status.equals(
                "COMPLETED"
        )) {

            TextView none =
                    makeText(
                            "No completed trips yet.\n\nComplete a trip to see your earnings.",
                            20
                    );

            none.setGravity(
                    Gravity.CENTER
            );

            layout.addView(none);

        } else {

            TextView earnings =
                    makeText(
                            "COMPLETED TRIPS\n\n" +
                            "Trips Completed: 1\n\n" +
                            "Total Fare:\n" +
                            "₱" +
                            fare +
                            "\n\n" +
                            "Driver Earnings:\n" +
                            "₱" +
                            fare +
                            "\n\n" +
                            (
                                rating.isEmpty()
                                ? "Latest Passenger Rating: Not rated"
                                : "Latest Passenger Rating: " +
                                  rating +
                                  " / 5"
                            ),
                            20
                    );

            earnings.setGravity(
                    Gravity.CENTER
            );

            earnings.setTypeface(
                    null,
                    Typeface.BOLD
            );

            earnings.setTextColor(
                    Color.rgb(
                            0,
                            130,
                            70
                    )
            );

            layout.addView(earnings);
        }

        addBackButton(layout);

        setContentView(layout);
    }

    private String getReadableStatus(
            String status
    ) {

        if (status.equals(
                "REQUESTED"
        )) {
            return "WAITING FOR DRIVER";
        }

        if (status.equals(
                "ACCEPTED"
        )) {
            return "RIDE ACCEPTED";
        }

        if (status.equals(
                "DRIVER_ON_THE_WAY"
        )) {
            return "DRIVER ON THE WAY";
        }

        if (status.equals(
                "DRIVER_ARRIVED"
        )) {
            return "DRIVER ARRIVED";
        }

        if (status.equals(
                "IN_PROGRESS"
        )) {
            return "TRIP IN PROGRESS";
        }

        if (status.equals(
                "COMPLETED"
        )) {
            return "RIDE COMPLETED";
        }

        if (status.equals(
                "CANCELLED"
        )) {
            return "RIDE CANCELLED";
        }

        if (status.equals(
                "DECLINED"
        )) {
            return "RIDE DECLINED";
        }

        return status;
    }

    private int getStatusColor(
            String status
    ) {

        if (status.equals(
                "COMPLETED"
        )) {

            return Color.rgb(
                    0,
                    150,
                    80
            );
        }

        if (status.equals(
                "CANCELLED"
        ) ||
                status.equals(
                        "DECLINED"
                )) {

            return Color.rgb(
                    200,
                    40,
                    40
            );
        }

        if (status.equals(
                "DRIVER_ARRIVED"
        )) {

            return Color.rgb(
                    220,
                    150,
                    0
            );
        }

        return Color.rgb(
                30,
                100,
                200
        );
    }

    private void logout() {

        prefs.edit()
                .remove("current_phone")
                .remove("current_name")
                .remove("current_role")
                .remove("driver_online")
                .apply();

        Intent intent =
                new Intent(
                        this,
                        MainActivity.class
                );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_NEW_TASK
        );

        startActivity(intent);

        finish();
    }
}
