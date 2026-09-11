
package com.sakyna.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class PassengerActivity extends Activity {

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
                        230,
                        120,
                        20
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

        TextView welcome =
                makeText(
                        "Passenger Dashboard",
                        21
                );

        welcome.setGravity(
                Gravity.CENTER
        );

        layout.addView(welcome);

        Button book =
                makeButton(
                        "🚕 Book a Ride"
                );

        book.setOnClickListener(
                v -> showBooking()
        );

        layout.addView(book);

        Button current =
                makeButton(
                        "📍 Current Ride"
                );

        current.setOnClickListener(
                v -> showCurrentRide()
        );

        layout.addView(current);

        Button history =
                makeButton(
                        "📋 Ride History"
                );

        history.setOnClickListener(
                v -> showHistory()
        );

        layout.addView(history);

        Button help =
                makeButton(
                        "🆘 Help / Emergency"
                );

        help.setOnClickListener(
                v -> Toast.makeText(
                        this,
                        "Emergency and support features will be connected later.",
                        Toast.LENGTH_LONG
                ).show()
        );

        layout.addView(help);

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

    private void showBooking() {

        LinearLayout layout =
                createPage(
                        "BOOK A RIDE"
                );

        EditText pickup =
                new EditText(this);

        pickup.setHint(
                "Enter pickup location"
        );

        pickup.setTextSize(17);

        layout.addView(pickup);

        EditText destination =
                new EditText(this);

        destination.setHint(
                "Enter destination"
        );

        destination.setTextSize(17);

        layout.addView(destination);

        TextView fare =
                makeText(
                        "Estimated Fare: ₱0",
                        20
                );

        fare.setTypeface(
                null,
                Typeface.BOLD
        );

        fare.setGravity(
                Gravity.CENTER
        );

        layout.addView(fare);

        Button calculate =
                makeButton(
                        "💰 Calculate Fare"
                );

        calculate.setOnClickListener(
                v -> {

                    String from =
                            pickup.getText()
                                    .toString()
                                    .trim();

                    String to =
                            destination.getText()
                                    .toString()
                                    .trim();

                    if (from.isEmpty() ||
                            to.isEmpty()) {

                        Toast.makeText(
                                this,
                                "Enter pickup and destination first.",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    int estimatedFare =
                            calculateFare(
                                    from,
                                    to
                            );

                    fare.setText(
                            "Estimated Fare: ₱" +
                            estimatedFare
                    );
                }
        );

        layout.addView(calculate);

        Button request =
                makeButton(
                        "🚕 REQUEST RIDE"
                );

        request.setOnClickListener(
                v -> {

                    String from =
                            pickup.getText()
                                    .toString()
                                    .trim();

                    String to =
                            destination.getText()
                                    .toString()
                                    .trim();

                    if (from.isEmpty() ||
                            to.isEmpty()) {

                        Toast.makeText(
                                this,
                                "Enter pickup and destination.",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    int estimatedFare =
                            calculateFare(
                                    from,
                                    to
                            );

                    String existingStatus =
                            prefs.getString(
                                    "ride_status",
                                    ""
                            );

                    if (existingStatus.equals(
                            "REQUESTED"
                    ) ||
                            existingStatus.equals(
                                    "ACCEPTED"
                            ) ||
                            existingStatus.equals(
                                    "DRIVER_ON_THE_WAY"
                            ) ||
                            existingStatus.equals(
                                    "DRIVER_ARRIVED"
                            ) ||
                            existingStatus.equals(
                                    "IN_PROGRESS"
                            )) {

                        Toast.makeText(
                                this,
                                "You already have an active ride.",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    prefs.edit()
                            .putString(
                                    "ride_pickup",
                                    from
                            )
                            .putString(
                                    "ride_destination",
                                    to
                            )
                            .putInt(
                                    "ride_fare",
                                    estimatedFare
                            )
                            .putString(
                                    "ride_status",
                                    "REQUESTED"
                            )
                            .remove(
                                    "ride_driver"
                            )
                            .remove(
                                    "ride_rating"
                            )
                            .apply();

                    Toast.makeText(
                            this,
                            "Ride requested successfully!",
                            Toast.LENGTH_LONG
                    ).show();

                    showCurrentRide();
                }
        );

        layout.addView(request);

        addBackButton(layout);

        setContentView(layout);
    }

    private int calculateFare(
            String pickup,
            String destination
    ) {

        /*
         * Prototype fare calculation.
         *
         * Later this will be replaced
         * with real GPS distance calculation.
         */

        int baseFare = 50;

        int extra =
                Math.abs(
                        pickup.length()
                        -
                        destination.length()
                );

        int fare =
                baseFare +
                (extra * 2);

        if (fare > 200) {
            fare = 200;
        }

        return fare;
    }

    private void showCurrentRide() {

        LinearLayout layout =
                createPage(
                        "CURRENT RIDE"
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
                            "No current ride.",
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

        TextView ride =
                makeText(
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
                            ? "Waiting for driver"
                            : driver
                        ) +
                        "\n\n" +
                        "RIDE STATUS\n" +
                        getReadableStatus(
                                status
                        ),
                        19
                );

        ride.setTypeface(
                null,
                Typeface.BOLD
        );

        layout.addView(ride);

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

        Button refresh =
                makeButton(
                        "🔄 Refresh Status"
                );

        refresh.setOnClickListener(
                v -> showCurrentRide()
        );

        layout.addView(refresh);

        if (status.equals(
                "REQUESTED"
        )) {

            Button cancel =
                    makeButton(
                            "❌ Cancel Ride"
                    );

            cancel.setOnClickListener(
                    v -> cancelRide()
            );

            layout.addView(cancel);
        }

        if (status.equals(
                "COMPLETED"
        )) {

            Button receipt =
                    makeButton(
                            "🧾 View Receipt"
                    );

            receipt.setOnClickListener(
                    v -> showReceipt()
            );

            layout.addView(receipt);

            Button rate =
                    makeButton(
                            "⭐ Rate Driver"
                    );

            rate.setOnClickListener(
                    v -> showRating()
            );

            layout.addView(rate);
        }

        addBackButton(layout);

        setContentView(layout);
    }

    private void cancelRide() {

        prefs.edit()
                .putString(
                        "ride_status",
                        "CANCELLED"
                )
                .apply();

        Toast.makeText(
                this,
                "Ride cancelled.",
                Toast.LENGTH_LONG
        ).show();

        showCurrentRide();
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
            return "DRIVER ACCEPTED";
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

    private void showReceipt() {

        LinearLayout layout =
                createPage(
                        "RIDE RECEIPT"
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
                        "ride_fare",
                        0
                );

        String rating =
                prefs.getString(
                        "ride_rating",
                        ""
                );

        TextView receipt =
                makeText(
                        "SAKAY NA\n\n" +
                        "RIDE RECEIPT\n\n" +
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
                        "Status:\n" +
                        "COMPLETED\n\n" +
                        (
                            rating.isEmpty()
                            ? "Rating: Not yet rated"
                            : "Rating: " +
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

    private void showRating() {

        LinearLayout layout =
                createPage(
                        "RATE DRIVER"
                );

        TextView question =
                makeText(
                        "How was your ride?",
                        21
                );

        question.setGravity(
                Gravity.CENTER
        );

        layout.addView(question);

        for (
                int rating = 1;
                rating <= 5;
                rating++
        ) {

            final int selectedRating =
                    rating;

            Button button =
                    makeButton(
                            "⭐ " +
                            rating +
                            " Star" +
                            (
                                rating == 1
                                ? ""
                                : "s"
                            )
                    );

            button.setOnClickListener(
                    v -> {

                        prefs.edit()
                                .putString(
                                        "ride_rating",
                                        String.valueOf(
                                                selectedRating
                                        )
                                )
                                .apply();

                        Toast.makeText(
                                this,
                                "Thank you for rating your driver!",
                                Toast.LENGTH_LONG
                        ).show();

                        showReceipt();
                    }
            );

            layout.addView(button);
        }

        addBackButton(layout);

        setContentView(layout);
    }

    private void showHistory() {

        LinearLayout layout =
                createPage(
                        "RIDE HISTORY"
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

        String rating =
                prefs.getString(
                        "ride_rating",
                        ""
                );

        if (pickup.isEmpty() ||
                destination.isEmpty()) {

            TextView none =
                    makeText(
                            "No rides in history yet.",
                            20
                    );

            none.setGravity(
                    Gravity.CENTER
            );

            layout.addView(none);

        } else {

            TextView history =
                    makeText(
                            "LATEST RIDE\n\n" +
                            "Pickup: " +
                            pickup +
                            "\n\n" +
                            "Destination: " +
                            destination +
                            "\n\n" +
                            "Driver: " +
                            (
                                driver.isEmpty()
                                ? "Not assigned"
                                : driver
                            ) +
                            "\n\n" +
                            "Fare: ₱" +
                            fare +
                            "\n\n" +
                            "Status: " +
                            getReadableStatus(
                                    status
                            ) +
                            "\n\n" +
                            (
                                rating.isEmpty()
                                ? "Rating: Not rated"
                                : "Rating: " +
                                  rating +
                                  " / 5"
                            ),
                            18
                    );

            layout.addView(history);
        }

        addBackButton(layout);

        setContentView(layout);
    }

    private void logout() {

        prefs.edit()
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
