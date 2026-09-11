
package com.sakyna.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class DriverActivity extends Activity {

    private boolean online = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showDriverHome();
    }

    private TextView makeText(String text, int size) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(Color.DKGRAY);
        view.setPadding(20, 20, 20, 20);
        return view;
    }

    private Button makeButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(17);
        button.setAllCaps(false);
        return button;
    }

    private void showDriverHome() {

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(25, 25, 25, 25);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = makeText("SAKAY NA", 30);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.rgb(30, 100, 200));
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        TextView subtitle =
                makeText("Driver Dashboard", 20);
        subtitle.setGravity(Gravity.CENTER);
        layout.addView(subtitle);

        TextView status =
                makeText(
                        online ? "Status: ONLINE" : "Status: OFFLINE",
                        20
                );

        status.setGravity(Gravity.CENTER);
        status.setTypeface(null, Typeface.BOLD);
        layout.addView(status);

        Button onlineButton =
                makeButton(
                        online ? "Go Offline" : "Go Online"
                );

        onlineButton.setOnClickListener(v -> {

            online = !online;

            if (online) {

                status.setText("Status: ONLINE");
                onlineButton.setText("Go Offline");

                Toast.makeText(
                        this,
                        "You are now accepting rides",
                        Toast.LENGTH_SHORT
                ).show();

            } else {

                status.setText("Status: OFFLINE");
                onlineButton.setText("Go Online");

                Toast.makeText(
                        this,
                        "You are now offline",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        layout.addView(onlineButton);

        Button requests =
                makeButton("Booking Requests");

        requests.setOnClickListener(v ->
                showBookingRequests()
        );

        layout.addView(requests);

        Button earnings =
                makeButton("Earnings & History");

        earnings.setOnClickListener(v ->
                showEarnings()
        );

        layout.addView(earnings);

        Button logout =
                makeButton("Logout");

        logout.setOnClickListener(v ->
                logout()
        );

        layout.addView(logout);

        setContentView(layout);
    }

    private void showBookingRequests() {

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(25, 25, 25, 25);

        TextView title =
                makeText("BOOKING REQUESTS", 27);

        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.rgb(30, 100, 200));
        title.setGravity(Gravity.CENTER);

        layout.addView(title);

        String pickup =
                getSharedPreferences(
                        "SakayNa",
                        MODE_PRIVATE
                ).getString(
                        "ride_pickup",
                        ""
                );

        String destination =
                getSharedPreferences(
                        "SakayNa",
                        MODE_PRIVATE
                ).getString(
                        "ride_destination",
                        ""
                );

        String status =
                getSharedPreferences(
                        "SakayNa",
                        MODE_PRIVATE
                ).getString(
                        "ride_status",
                        ""
                );

        int fare =
                getSharedPreferences(
                        "SakayNa",
                        MODE_PRIVATE
                ).getInt(
                        "ride_fare",
                        0
                );

        if (pickup.isEmpty() ||
                destination.isEmpty() ||
                !status.equals("REQUESTED")) {

            TextView noRide =
                    makeText(
                            "No new booking requests.",
                            20
                    );

            noRide.setGravity(Gravity.CENTER);

            layout.addView(noRide);

        } else {

            TextView request =
                    makeText(
                            "NEW RIDE REQUEST\n\n" +
                            "Pickup:\n" +
                            pickup +
                            "\n\nDestination:\n" +
                            destination +
                            "\n\nEstimated Fare: ₱" +
                            fare,
                            19
                    );

            request.setTypeface(
                    null,
                    Typeface.BOLD
            );

            layout.addView(request);

            Button accept =
                    makeButton("ACCEPT RIDE");

            accept.setOnClickListener(v ->
                    acceptRide()
            );

            layout.addView(accept);

            Button decline =
                    makeButton("DECLINE RIDE");

            decline.setOnClickListener(v ->
                    declineRide()
            );

            layout.addView(decline);
        }

        Button back =
                makeButton("Back to Dashboard");

        back.setOnClickListener(v ->
                showDriverHome()
        );

        layout.addView(back);

        setContentView(layout);
    }

    private void acceptRide() {

        getSharedPreferences(
                "SakayNa",
                MODE_PRIVATE
        )
                .edit()
                .putString(
                        "ride_status",
                        "ACCEPTED"
                )
                .putString(
                        "ride_driver",
                        getDriverName()
                )
                .apply();

        Toast.makeText(
                this,
                "Ride accepted!",
                Toast.LENGTH_LONG
        ).show();

        showDriverTrip();
    }

    private void declineRide() {

        getSharedPreferences(
                "SakayNa",
                MODE_PRIVATE
        )
                .edit()
                .putString(
                        "ride_status",
                        "DECLINED"
                )
                .apply();

        Toast.makeText(
                this,
                "Ride declined.",
                Toast.LENGTH_SHORT
        ).show();

        showBookingRequests();
    }

    private String getDriverName() {

        return getSharedPreferences(
                "SakayNa",
                MODE_PRIVATE
        ).getString(
                "current_name",
                "Sakay Na Driver"
        );
    }

    private void showDriverTrip() {

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(25, 25, 25, 25);

        TextView title =
                makeText("CURRENT TRIP", 28);

        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.rgb(30, 100, 200));
        title.setGravity(Gravity.CENTER);

        layout.addView(title);

        String pickup =
                getSharedPreferences(
                        "SakayNa",
                        MODE_PRIVATE
                ).getString(
                        "ride_pickup",
                        ""
                );

        String destination =
                getSharedPreferences(
                        "SakayNa",
                        MODE_PRIVATE
                ).getString(
                        "ride_destination",
                        ""
                );

        String status =
                getSharedPreferences(
                        "SakayNa",
                        MODE_PRIVATE
                ).getString(
                        "ride_status",
                        ""
                );

        int fare =
                getSharedPreferences(
                        "SakayNa",
                        MODE_PRIVATE
                ).getInt(
                        "ride_fare",
                        0
                );

        TextView route =
                makeText(
                        "Pickup:\n" +
                        pickup +
                        "\n\nDestination:\n" +
                        destination +
                        "\n\nFare: ₱" +
                        fare +
                        "\n\nStatus:\n" +
                        status,
                        19
                );

        layout.addView(route);

        if (status.equals("ACCEPTED")) {

            Button onTheWay =
                    makeButton("DRIVER ON THE WAY");

            onTheWay.setOnClickListener(v -> {

                updateRideStatus(
                        "DRIVER_ON_THE_WAY"
                );

                showDriverTrip();
            });

            layout.addView(onTheWay);

        } else if (status.equals("DRIVER_ON_THE_WAY")) {

            Button arrived =
                    makeButton("DRIVER ARRIVED");

            arrived.setOnClickListener(v -> {

                updateRideStatus(
                        "DRIVER_ARRIVED"
                );

                showDriverTrip();
            });

            layout.addView(arrived);

        } else if (status.equals("DRIVER_ARRIVED")) {

            Button start =
                    makeButton("START TRIP");

            start.setOnClickListener(v -> {

                updateRideStatus(
                        "IN_PROGRESS"
                );

                showDriverTrip();
            });

            layout.addView(start);

        } else if (status.equals("IN_PROGRESS")) {

            Button finish =
                    makeButton("FINISH TRIP");

            finish.setOnClickListener(v ->
                    finishTrip()
            );

            layout.addView(finish);

        } else if (status.equals("COMPLETED")) {

            TextView completed =
                    makeText(
                            "TRIP COMPLETED\n\n" +
                            "Fare collected: ₱" +
                            fare,
                            20
                    );

            completed.setTypeface(
                    null,
                    Typeface.BOLD
            );

            completed.setGravity(Gravity.CENTER);

            layout.addView(completed);
        }

        Button back =
                makeButton("Back to Dashboard");

        back.setOnClickListener(v ->
                showDriverHome()
        );

        layout.addView(back);

        setContentView(layout);
    }

    private void updateRideStatus(String newStatus) {

        getSharedPreferences(
                "SakayNa",
                MODE_PRIVATE
        )
                .edit()
                .putString(
                        "ride_status",
                        newStatus
                )
                .apply();

        Toast.makeText(
                this,
                "Ride status: " + newStatus,
                Toast.LENGTH_SHORT
        ).show();
    }

    private void finishTrip() {

        getSharedPreferences(
                "SakayNa",
                MODE_PRIVATE
        )
                .edit()
                .putString(
                        "ride_status",
                        "COMPLETED"
                )
                .apply();

        Toast.makeText(
                this,
                "Trip completed successfully!",
                Toast.LENGTH_LONG
        ).show();

        showDriverTrip();
    }

    private void showEarnings() {

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(25, 25, 25, 25);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title =
                makeText("EARNINGS & HISTORY", 27);

        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.rgb(30, 100, 200));
        title.setGravity(Gravity.CENTER);

        layout.addView(title);

        String status =
                getSharedPreferences(
                        "SakayNa",
                        MODE_PRIVATE
                ).getString(
                        "ride_status",
                        ""
                );

        int fare =
                getSharedPreferences(
                        "SakayNa",
                        MODE_PRIVATE
                ).getInt(
                        "ride_fare",
                        0
                );

        String earningsText;

        if (status.equals("COMPLETED")) {

            earningsText =
                    "Completed Rides: 1\n\n" +
                    "Total Earnings: ₱" +
                    fare;

        } else {

            earningsText =
                    "Completed Rides: 0\n\n" +
                    "Total Earnings: ₱0";
        }

        TextView earnings =
                makeText(
                        earningsText,
                        21
                );

        earnings.setGravity(Gravity.CENTER);

        layout.addView(earnings);

        Button back =
                makeButton("Back to Dashboard");

        back.setOnClickListener(v ->
                showDriverHome()
        );

        layout.addView(back);

        setContentView(layout);
    }

    private void logout() {

        getSharedPreferences(
                "SakayNa",
                MODE_PRIVATE
        )
                .edit()
                .remove("current_phone")
                .remove("current_name")
                .remove("current_role")
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
