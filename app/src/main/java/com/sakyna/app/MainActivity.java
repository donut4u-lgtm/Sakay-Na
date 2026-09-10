
package com.sakyna.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private final int GREEN = Color.rgb(20, 110, 60);
    private EditText pickupInput;
    private EditText destinationInput;
    private TextView fareText;

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
                menuButton(
                        "🧍  PASSENGER",
                        GREEN
                );

        Button driver =
                menuButton(
                        "🛺  DRIVER",
                        Color.rgb(35, 95, 170)
                );

        Button admin =
                menuButton(
                        "🛡  ADMIN",
                        Color.rgb(170, 60, 50)
                );

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
                label(
                        "BOOK A RIDE",
                        28,
                        GREEN,
                        true
                )
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

        pickupInput =
                input("Example: Sariaya Public Market");

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

        estimate.setOnClickListener(
                v -> calculateFare()
        );

        Button request =
                menuButton(
                        "REQUEST RIDE",
                        GREEN
                );

        request.setOnClickListener(
                v -> requestRide()
        );

        Button back =
                menuButton(
                        "BACK",
                        Color.GRAY
                );

        back.setOnClickListener(
                v -> showHome()
        );

        screen.addView(estimate);
        screen.addView(request);
        screen.addView(back);

        setContentView(screen);
    }

    private void calculateFare() {

        String pickup = pickupInput.getText().toString().trim();
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

        /*
         * Initial Sakay Na fare model.
         * This is only a temporary local calculation.
         * Later it will be controlled by the Admin/Firebase.
         */
        int baseFare = 50;

        fareText.setText(
                "Estimated fare: ₱" + baseFare
        );

        Toast.makeText(
                this,
                "Fare calculated.",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void requestRide() {

        String pickup = pickupInput.getText().toString().trim();
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

        showRideRequested(pickup, destination);
    }

    private void showRideRequested(
            String pickup,
            String destination
    ) {

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
                label(
                        "🛺",
                        55,
                        Color.BLACK,
                        false
                )
        );

        TextView details =
                label(
                        "Pickup:\n" + pickup +
                        "\n\nDestination:\n" +
                        destination,
                        18,
                        Color.DKGRAY,
                        false
                );

        details.setGravity(Gravity.CENTER);
        details.setPadding(0, 25, 0, 25);

        screen.addView(details);

        screen.addView(
                label(
                        "Searching for an available driver...",
                        17,
                        Color.DKGRAY,
                        false
                )
        );

        Button cancel =
                menuButton(
                        "CANCEL RIDE",
                        Color.rgb(170, 60, 50)
                );

        cancel.setOnClickListener(
                v -> showPassenger()
        );

        screen.addView(cancel);

        setContentView(screen);
    }

    private void showDriver() {

        LinearLayout screen = baseScreen();

        screen.addView(
                label(
                        "DRIVER MODE",
                        28,
                        Color.rgb(35, 95, 170),
                        true
                )
        );

        screen.addView(
                label(
                        "Ready to receive ride requests",
                        17,
                        Color.DKGRAY,
                        false
                )
        );

        Button online =
                menuButton(
                        "GO ONLINE",
                        Color.rgb(35, 95, 170)
                );

        online.setOnClickListener(
                v -> showDriverOnline()
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

    private void showDriverOnline() {

        LinearLayout screen = baseScreen();

        screen.addView(
                label(
                        "🟢 DRIVER ONLINE",
                        26,
                        Color.rgb(35, 95, 170),
                        true
                )
        );

        screen.addView(
                label(
                        "Waiting for ride requests...",
                        18,
                        Color.DKGRAY,
                        false
                )
        );

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

    private void showAdmin() {

        LinearLayout screen = baseScreen();

        screen.addView(
                label(
                        "ADMIN DASHBOARD",
                        28,
                        Color.rgb(170, 60, 50),
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

        screen.addView(
                label(
                        "\nPassengers: 0\n\nDrivers: 0\n\nActive Rides: 0\n\nCompleted Rides: 0",
                        18,
                        Color.BLACK,
                        false
                )
        );

        Button back =
                menuButton(
                        "BACK",
                        Color.GRAY
                );

        back.setOnClickListener(
                v -> showHome()
        );

        screen.addView(back);

        setContentView(screen);
    }
}
