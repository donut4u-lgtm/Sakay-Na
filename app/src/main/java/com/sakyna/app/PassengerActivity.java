
package com.sakyna.app;

import android.app.Activity;
import android.content.Intent;
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

    private EditText pickupInput;
    private EditText destinationInput;
    private TextView fareText;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showPassengerHome();
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

    private EditText makeInput(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setTextSize(17);
        input.setPadding(20, 15, 20, 15);
        return input;
    }

    private void showPassengerHome() {

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(25, 25, 25, 25);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = makeText("SAKAY NA", 30);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.rgb(0, 150, 80));
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        TextView subtitle =
                makeText("Passenger Dashboard", 20);
        subtitle.setGravity(Gravity.CENTER);
        layout.addView(subtitle);

        Button book = makeButton("Book a Ride");
        book.setOnClickListener(v -> showBooking());
        layout.addView(book);

        Button status = makeButton("Current Ride");
        status.setOnClickListener(v -> showRideStatus());
        layout.addView(status);

        Button history = makeButton("Ride History");
        history.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "Ride history will be added next.",
                        Toast.LENGTH_SHORT
                ).show()
        );
        layout.addView(history);

        Button help = makeButton("Help / Emergency");
        help.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "Emergency help will be added next.",
                        Toast.LENGTH_SHORT
                ).show()
        );
        layout.addView(help);

        Button logout = makeButton("Logout");
        logout.setOnClickListener(v -> logout());
        layout.addView(logout);

        setContentView(layout);
    }

    private void showBooking() {

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(25, 25, 25, 25);

        TextView title = makeText("BOOK A RIDE", 28);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.rgb(0, 150, 80));
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        TextView instruction =
                makeText(
                        "Where are you going?",
                        19
                );
        instruction.setGravity(Gravity.CENTER);
        layout.addView(instruction);

        pickupInput =
                makeInput("Pickup location");

        layout.addView(pickupInput);

        destinationInput =
                makeInput("Destination");

        layout.addView(destinationInput);

        fareText =
                makeText(
                        "Estimated Fare: ₱0",
                        21
                );

        fareText.setTypeface(null, Typeface.BOLD);
        fareText.setTextColor(Color.rgb(0, 120, 70));
        fareText.setGravity(Gravity.CENTER);
        layout.addView(fareText);

        Button calculate =
                makeButton("Calculate Fare");

        calculate.setOnClickListener(v ->
                calculateFare()
        );

        layout.addView(calculate);

        Button request =
                makeButton("REQUEST RIDE");

        request.setOnClickListener(v ->
                requestRide()
        );

        layout.addView(request);

        Button back =
                makeButton("Back");

        back.setOnClickListener(v ->
                showPassengerHome()
        );

        layout.addView(back);

        setContentView(layout);
    }

    private void calculateFare() {

        String pickup =
                pickupInput.getText().toString().trim();

        String destination =
                destinationInput.getText().toString().trim();

        if (pickup.isEmpty() ||
                destination.isEmpty()) {

            Toast.makeText(
                    this,
                    "Enter pickup and destination first.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        /*
         * Prototype fare.
         *
         * Later this will use actual
         * distance from GPS/maps.
         */
        int fare = 50;

        fareText.setText(
                "Estimated Fare: ₱" + fare
        );

        Toast.makeText(
                this,
                "Fare calculated.",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void requestRide() {

        String pickup =
                pickupInput.getText().toString().trim();

        String destination =
                destinationInput.getText().toString().trim();

        if (pickup.isEmpty() ||
                destination.isEmpty()) {

            Toast.makeText(
                    this,
                    "Enter pickup and destination first.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        int fare = 50;

        getSharedPreferences(
                "SakayNa",
                MODE_PRIVATE
        )
                .edit()
                .putString("ride_pickup", pickup)
                .putString(
                        "ride_destination",
                        destination
                )
                .putInt("ride_fare", fare)
                .putString(
                        "ride_status",
                        "REQUESTED"
                )
                .putString(
                        "ride_driver",
                        ""
                )
                .apply();

        Toast.makeText(
                this,
                "Ride requested successfully!",
                Toast.LENGTH_LONG
        ).show();

        showRideStatus();
    }

    private void showRideStatus() {

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(25, 25, 25, 25);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title =
                makeText("CURRENT RIDE", 28);

        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.rgb(0, 150, 80));
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

        String driver =
                getSharedPreferences(
                        "SakayNa",
                        MODE_PRIVATE
                ).getString(
                        "ride_driver",
                        ""
                );

        if (pickup.isEmpty() ||
                destination.isEmpty() ||
                status.isEmpty()) {

            TextView none =
                    makeText(
                            "No active ride.",
                            20
                    );

            none.setGravity(Gravity.CENTER);
            layout.addView(none);

        } else {

            TextView route =
                    makeText(
                            "Pickup:\n" + pickup +
                            "\n\nDestination:\n" +
                            destination,
                            18
                    );

            layout.addView(route);

            statusText =
                    makeText(
                            "Ride Status: " + status,
                            21
                    );

            statusText.setTypeface(
                    null,
                    Typeface.BOLD
            );

            statusText.setTextColor(
                    Color.rgb(0, 120, 70)
            );

            statusText.setGravity(Gravity.CENTER);

            layout.addView(statusText);

            TextView fareView =
                    makeText(
                            "Fare: ₱" + fare,
                            20
                    );

            fareView.setGravity(Gravity.CENTER);
            layout.addView(fareView);

            if (!driver.isEmpty()) {

                TextView driverView =
                        makeText(
                                "Driver: " + driver,
                                18
                        );

                driverView.setGravity(Gravity.CENTER);

                layout.addView(driverView);
            }

            if (status.equals("REQUESTED") ||
                    status.equals("ACCEPTED") ||
                    status.equals("DRIVER_ON_THE_WAY") ||
                    status.equals("DRIVER_ARRIVED")) {

                Button cancel =
                        makeButton("CANCEL RIDE");

                cancel.setOnClickListener(v ->
                        cancelRide()
                );

                layout.addView(cancel);
            }

            if (status.equals("COMPLETED")) {

                Button receipt =
                        makeButton("View Receipt");

                receipt.setOnClickListener(v ->
                        showReceipt()
                );

                layout.addView(receipt);
            }
        }

        Button back =
                makeButton("Back to Dashboard");

        back.setOnClickListener(v ->
                showPassengerHome()
        );

        layout.addView(back);

        setContentView(layout);
    }

    private void cancelRide() {

        getSharedPreferences(
                "SakayNa",
                MODE_PRIVATE
        )
                .edit()
                .putString(
                        "ride_status",
                        "CANCELLED"
                )
                .apply();

        Toast.makeText(
                this,
                "Ride cancelled.",
                Toast.LENGTH_SHORT
        ).show();

        showRideStatus();
    }

    private void showReceipt() {

        int fare =
                getSharedPreferences(
                        "SakayNa",
                        MODE_PRIVATE
                ).getInt(
                        "ride_fare",
                        0
                );

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 30, 30, 30);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title =
                makeText("RIDE RECEIPT", 28);

        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.rgb(0, 150, 80));
        title.setGravity(Gravity.CENTER);

        layout.addView(title);

        TextView receipt =
                makeText(
                        "SAKAY NA\n\n" +
                        "Ride completed\n\n" +
                        "Total Fare: ₱" + fare +
                        "\n\nThank you for riding with Sakay Na!",
                        20
                );

        receipt.setGravity(Gravity.CENTER);
        layout.addView(receipt);

        Button done =
                makeButton("Back to Dashboard");

        done.setOnClickListener(v ->
                showPassengerHome()
        );

        layout.addView(done);

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
