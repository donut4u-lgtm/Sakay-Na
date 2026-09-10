
package com.sakyna.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private final int GREEN = Color.rgb(20, 110, 60);
    private final int BLUE = Color.rgb(35, 95, 170);
    private final int RED = Color.rgb(170, 60, 50);

    private EditText pickupInput;
    private EditText destinationInput;
    private TextView fareText;

    // Temporary local ride request.
    // Firebase will replace this later so different phones can communicate.
    private static boolean rideRequested = false;
    private static boolean rideAccepted = false;

    private static String requestedPickup = "";
    private static String requestedDestination = "";
    private static int requestedFare = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showHome();
    }

    private TextView label(String text, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER);

        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }

        return view;
    }

    private Button menuButton(String text, int color) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(17);
        button.setTextColor(Color.WHITE);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(24);
        button.setBackground(background);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        65
                );

        params.setMargins(0, 8, 0, 8);
        button.setLayoutParams(params);

        return button;
    }

    private EditText input(String hint) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setTextSize(17);
        editText.setSingleLine(true);
        editText.setPadding(20, 10, 20, 10);

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(245, 245, 245));
        background.setCornerRadius(18);
        background.setStroke(1, Color.LTGRAY);

        editText.setBackground(background);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        60
                );

        params.setMargins(0, 5, 0, 15);
        editText.setLayoutParams(params);

        return editText;
    }

    private LinearLayout baseScreen() {
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setPadding(25, 30, 25, 25);
        screen.setBackgroundColor(Color.WHITE);
        return screen;
    }

    private void showHome() {

        LinearLayout screen = baseScreen();
        screen.setGravity(Gravity.CENTER_HORIZONTAL);

        screen.addView(label("🛺", 55, Color.BLACK, false));
        screen.addView(label("Sakay Na", 36, GREEN, true));
        screen.addView(
                label(
                        "Your local ride, made easy",
                        17,
                        Color.DKGRAY,
                        false
                )
        );

        TextView choose =
                label(
                        "Choose your mode",
                        20,
                        Color.BLACK,
                        true
                );

        choose.setPadding(0, 40, 0, 15);
        screen.addView(choose);

        Button passenger =
                menuButton("🧍  PASSENGER", GREEN);

        Button driver =
                menuButton("🛺  DRIVER", BLUE);

        Button admin =
                menuButton("🛡  ADMIN", RED);

        passenger.setOnClickListener(v -> showPassenger());
        driver.setOnClickListener(v -> showDriver());
        admin.setOnClickListener(v -> showAdmin());

        screen.addView(passenger);
        screen.addView(driver);
        screen.addView(admin);

        setContentView(screen);
    }

    private void showPassenger() {

        LinearLayout screen = baseScreen();

        screen.addView(
                label("BOOK A RIDE", 28, GREEN, true)
        );

        TextView instruction =
                label(
                        "Tell us where you're going.",
                        17,
                        Color.DKGRAY,
                        false
                );

        instruction.setPadding(0, 5, 0, 25);
        screen.addView(instruction);

        screen.addView(
                label(
                        "PICKUP LOCATION",
                        15,
                        Color.BLACK,
                        true
                )
        );

        pickupInput = input("Example: Sariaya Public Market");
        screen.addView(pickupInput);

        screen.addView(
                label(
                        "DESTINATION",
                        15,
                        Color.BLACK,
                        true
                )
        );

        destinationInput =
                input("Example: Sariaya Municipal Hall");

        screen.addView(destinationInput);

        screen.addView(
                label(
                        "RIDE TYPE",
                        15,
                        Color.BLACK,
                        true
                )
        );

        TextView rideType =
                label(
                        "🛺  Tricycle",
                        18,
                        GREEN,
                        true
                );

        rideType.setGravity(Gravity.LEFT);
        rideType.setPadding(15, 15, 15, 15);
        screen.addView(rideType);

        fareText =
                label(
                        "Estimated fare: ₱0",
                        20,
                        GREEN,
                        true
                );

        fareText.setPadding(0, 20, 0, 10);
        screen.addView(fareText);

        Button estimate =
                menuButton(
                        "CALCULATE FARE",
                        Color.rgb(40, 120, 70)
                );

        estimate.setOnClickListener(v -> calculateFare());

        Button request =
                menuButton("REQUEST RIDE", GREEN);

        request.setOnClickListener(v -> requestRide());

        Button status =
                menuButton(
                        "CHECK RIDE STATUS",
                        BLUE
                );

        status.setOnClickListener(v -> showPassengerStatus());

        Button back =
                menuButton("BACK", Color.GRAY);

        back.setOnClickListener(v -> showHome());

        screen.addView(estimate);
        screen.addView(request);
        screen.addView(status);
        screen.addView(back);

        setContentView(screen);
    }

    private void calculateFare() {

        String pickup =
                pickupInput.getText().toString().trim();

        String destination =
                destinationInput.getText().toString().trim();

        if (pickup.isEmpty() || destination.isEmpty()) {
            Toast.makeText(
                    this,
                    "Please enter pickup and destination.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        requestedFare = 50;

        fareText.setText(
                "Estimated fare: ₱" + requestedFare
        );
    }

    private void requestRide() {

        String pickup =
                pickupInput.getText().toString().trim();

        String destination =
                destinationInput.getText().toString().trim();

        if (pickup.isEmpty() || destination.isEmpty()) {
            Toast.makeText(
                    this,
                    "Please enter pickup and destination.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        requestedPickup = pickup;
        requestedDestination = destination;

        if (requestedFare == 0) {
            requestedFare = 50;
        }

        rideRequested = true;
        rideAccepted = false;

        showRideRequested();
    }

    private void showRideRequested() {

        LinearLayout screen = baseScreen();
        screen.setGravity(Gravity.CENTER_HORIZONTAL);

        screen.addView(
                label(
                        "RIDE REQUESTED",
                        28,
                        GREEN,
                        true
                )
        );

        screen.addView(
                label("🛺", 55, Color.BLACK, false)
        );

        screen.addView(
                label(
                        "Searching for an available driver...",
                        17,
                        Color.DKGRAY,
                        false
                )
        );

        TextView details =
                label(
                        "\nPickup:\n" +
                        requestedPickup +
                        "\n\nDestination:\n" +
                        requestedDestination +
                        "\n\nFare: ₱" +
                        requestedFare,
                        18,
                        Color.DKGRAY,
                        false
                );

        details.setGravity(Gravity.CENTER);
        screen.addView(details);

        Button status =
                menuButton(
                        "CHECK RIDE STATUS",
                        BLUE
                );

        status.setOnClickListener(
                v -> showPassengerStatus()
        );

        Button cancel =
                menuButton(
                        "CANCEL RIDE",
                        RED
                );

        cancel.setOnClickListener(v -> {
            rideRequested = false;
            rideAccepted = false;
            showPassenger();
        });

        screen.addView(status);
        screen.addView(cancel);

        setContentView(screen);
    }

    private void showPassengerStatus() {

        LinearLayout screen = baseScreen();
        screen.setGravity(Gravity.CENTER_HORIZONTAL);

        screen.addView(
                label(
                        "RIDE STATUS",
                        28,
                        GREEN,
                        true
                )
        );

        if (!rideRequested) {

            screen.addView(
                    label(
                            "\nNo active ride.\n\nBook a ride first.",
                            19,
                            Color.DKGRAY,
                            false
                    )
            );

        } else if (!rideAccepted) {

            screen.addView(
                    label(
                            "\n🟡 DRIVER SEARCHING\n\n" +
                            "Your ride request is waiting for a driver.",
                            19,
                            Color.DKGRAY,
                            false
                    )
            );

        } else {

            screen.addView(
                    label(
                            "\n🟢 DRIVER ACCEPTED\n\n" +
                            "Your driver has accepted the ride!\n\n" +
                            "Pickup:\n" +
                            requestedPickup +
                            "\n\nDestination:\n" +
                            requestedDestination +
                            "\n\nFare: ₱" +
                            requestedFare,
                            19,
                            GREEN,
                            true
                    )
            );
        }

        Button back =
                menuButton("BACK", Color.GRAY);

        back.setOnClickListener(v -> showPassenger());

        screen.addView(back);

        setContentView(screen);
    }

    private void showDriver() {

        LinearLayout screen = baseScreen();

        screen.addView(
                label(
                        "DRIVER MODE",
                        28,
                        BLUE,
                        true
                )
        );

        screen.addView(
                label(
                        "Manage incoming ride requests",
                        17,
                        Color.DKGRAY,
                        false
                )
        );

        Button online =
                menuButton(
                        "GO ONLINE",
                        BLUE
                );

        online.setOnClickListener(
                v -> showDriverRequests()
        );

        Button back =
                menuButton(
                        "BACK",
                        Color.GRAY
                );

        back.setOnClickListener(
                v -> showHome()
        );

        screen.addView(online);
        screen.addView(back);

        setContentView(screen);
    }

    private void showDriverRequests() {

        LinearLayout screen = baseScreen();

        screen.addView(
                label(
                        "🟢 DRIVER ONLINE",
                        26,
                        BLUE,
                        true
                )
        );

        if (!rideRequested) {

            screen.addView(
                    label(
                            "\nNo ride requests yet.\n\n" +
                            "Waiting for passengers...",
                            19,
                            Color.DKGRAY,
                            false
                    )
            );

        } else if (rideAccepted) {

            screen.addView(
                    label(
                            "\nRIDE ACCEPTED\n\n" +
                            "Passenger pickup:\n" +
                            requestedPickup +
                            "\n\nDestination:\n" +
                            requestedDestination +
                            "\n\nFare: ₱" +
                            requestedFare,
                            19,
                            GREEN,
                            true
                    )
            );

            Button start =
                    menuButton(
                            "START RIDE",
                            BLUE
                    );

            start.setOnClickListener(
                    v -> showRideStarted()
            );

            screen.addView(start);

        } else {

            screen.addView(
                    label(
                            "\n🔔 NEW RIDE REQUEST",
                            23,
                            BLUE,
                            true
                    )
            );

            screen.addView(
                    label(
                            "\nPickup:\n" +
                            requestedPickup +
                            "\n\nDestination:\n" +
                            requestedDestination +
                            "\n\nFare: ₱" +
                            requestedFare,
                            19,
                            Color.DKGRAY,
                            false
                    )
            );

            Button accept =
                    menuButton(
                            "ACCEPT RIDE",
                            BLUE
                    );

            accept.setOnClickListener(v -> {
                rideAccepted = true;

                Toast.makeText(
                        this,
                        "Ride accepted!",
                        Toast.LENGTH_SHORT
                ).show();

                showDriverRequests();
            });

            screen.addView(accept);
        }

        Button offline =
                menuButton(
                        "GO OFFLINE",
                        Color.GRAY
                );

        offline.setOnClickListener(
                v -> showDriver()
        );

        screen.addView(offline);

        setContentView(screen);
    }

    private void showRideStarted() {

        LinearLayout screen = baseScreen();
        screen.setGravity(Gravity.CENTER_HORIZONTAL);

        screen.addView(
                label(
                        "🚕 RIDE IN PROGRESS",
                        27,
                        BLUE,
                        true
                )
        );

        screen.addView(
                label(
                        "\nPickup:\n" +
                        requestedPickup +
                        "\n\nDestination:\n" +
                        requestedDestination +
                        "\n\nFare: ₱" +
                        requestedFare,
                        19,
                        Color.DKGRAY,
                        false
                )
        );

        Button complete =
                menuButton(
                        "COMPLETE RIDE",
                        GREEN
                );

        complete.setOnClickListener(
                v -> completeRide()
        );

        screen.addView(complete);

        setContentView(screen);
    }

    private void completeRide() {

        rideRequested = false;
        rideAccepted = false;

        Toast.makeText(
                this,
                "Ride completed!",
                Toast.LENGTH_SHORT
        ).show();

        showHome();
    }

    private void showAdmin() {

        LinearLayout screen = baseScreen();

        screen.addView(
                label(
                        "ADMIN DASHBOARD",
                        28,
                        RED,
                        true
                )
        );

        screen.addView(
                label(
                        "Sakay Na Management",
                        18,
                        Color.DKGRAY,
                        false
                )
        );

        String status;

        if (rideRequested && !rideAccepted) {
            status = "🟡 Ride Waiting for Driver";
        } else if (rideRequested) {
            status = "🟢 Ride Accepted";
        } else {
            status = "⚪ No Active Ride";
        }

        screen.addView(
                label(
                        "\nPassengers: 1\n\n" +
                        "Drivers: 1\n\n" +
                        "Active Ride:\n" +
                        status,
                        18,
                        Color.BLACK,
                        false
                )
        );

        Button refresh =
                menuButton(
                        "REFRESH",
                        RED
                );

        refresh.setOnClickListener(
                v -> showAdmin()
        );

        Button back =
                menuButton(
                        "BACK",
                        Color.GRAY
                );

        back.setOnClickListener(
                v -> showHome()
        );

        screen.addView(refresh);
        screen.addView(back);

        setContentView(screen);
    }
}
