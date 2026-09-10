package com.sakyna.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private final int GREEN = Color.rgb(20, 110, 60);

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
                        70
                );

        params.setMargins(0, 8, 0, 8);
        button.setLayoutParams(params);

        return button;
    }

    private LinearLayout baseScreen() {
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setGravity(Gravity.CENTER_HORIZONTAL);
        screen.setPadding(30, 40, 30, 30);
        screen.setBackgroundColor(Color.WHITE);
        return screen;
    }

    private void showHome() {

        LinearLayout screen = baseScreen();

        screen.addView(label("🛺", 58, Color.BLACK, false));
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
                        "How would you like to use Sakay Na?",
                        19,
                        Color.BLACK,
                        true
                );

        choose.setPadding(0, 45, 0, 20);
        screen.addView(choose);

        Button passenger =
                menuButton("🧍  PASSENGER", GREEN);

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

        passenger.setOnClickListener(
                view -> showPassenger()
        );

        driver.setOnClickListener(
                view -> showDriver()
        );

        admin.setOnClickListener(
                view -> showAdmin()
        );

        screen.addView(passenger);
        screen.addView(driver);
        screen.addView(admin);

        setContentView(screen);
    }

    private void showPassenger() {

        LinearLayout screen = baseScreen();

        screen.addView(
                label(
                        "PASSENGER",
                        30,
                        GREEN,
                        true
                )
        );

        screen.addView(
                label(
                        "Where do you want to go?",
                        20,
                        Color.BLACK,
                        true
                )
        );

        Button book =
                menuButton(
                        "BOOK A RIDE",
                        GREEN
                );

        book.setOnClickListener(
                view -> showBooking()
        );

        Button back =
                menuButton(
                        "BACK",
                        Color.GRAY
                );

        back.setOnClickListener(
                view -> showHome()
        );

        screen.addView(book);
        screen.addView(back);

        setContentView(screen);
    }

    private void showBooking() {

        LinearLayout screen = baseScreen();

        screen.addView(
                label(
                        "BOOK A RIDE",
                        30,
                        GREEN,
                        true
                )
        );

        screen.addView(
                label(
                        "Pickup",
                        18,
                        Color.BLACK,
                        true
                )
        );

        TextView pickup =
                label(
                        "Set your pickup location",
                        17,
                        Color.DKGRAY,
                        false
                );

        pickup.setPadding(0, 15, 0, 25);

        screen.addView(pickup);

        screen.addView(
                label(
                        "Destination",
                        18,
                        Color.BLACK,
                        true
                )
        );

        TextView destination =
                label(
                        "Set your destination",
                        17,
                        Color.DKGRAY,
                        false
                );

        destination.setPadding(0, 15, 0, 25);

        screen.addView(destination);

        Button request =
                menuButton(
                        "REQUEST RIDE",
                        GREEN
                );

        request.setOnClickListener(
                view -> showRideRequested()
        );

        Button back =
                menuButton(
                        "BACK",
                        Color.GRAY
                );

        back.setOnClickListener(
                view -> showPassenger()
        );

        screen.addView(request);
        screen.addView(back);

        setContentView(screen);
    }

    private void showRideRequested() {

        LinearLayout screen = baseScreen();

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
                        "Looking for an available driver...",
                        18,
                        Color.DKGRAY,
                        false
                )
        );

        screen.addView(
                label(
                        "Please wait.",
                        18,
                        Color.BLACK,
                        false
                )
        );

        Button cancel =
                menuButton(
                        "CANCEL RIDE",
                        Color.rgb(170, 60, 50)
                );

        cancel.setOnClickListener(
                view -> showPassenger()
        );

        screen.addView(cancel);

        setContentView(screen);
    }

    private void showDriver() {

        LinearLayout screen = baseScreen();

        screen.addView(
                label(
                        "DRIVER MODE",
                        30,
                        Color.rgb(35, 95, 170),
                        true
                )
        );

        screen.addView(
                label(
                        "Driver dashboard",
                        19,
                        Color.BLACK,
                        true
                )
        );

        Button online =
                menuButton(
                        "GO ONLINE",
                        Color.rgb(35, 95, 170)
                );

        online.setOnClickListener(
                view -> showDriverOnline()
        );

        Button back =
                menuButton(
                        "BACK",
                        Color.GRAY
                );

        back.setOnClickListener(
                view -> showHome()
        );

        screen.addView(online);
        screen.addView(back);

        setContentView(screen);
    }

    private void showDriverOnline() {

        LinearLayout screen = baseScreen();

        screen.addView(
                label(
                        "DRIVER ONLINE",
                        28,
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
                view -> showDriver()
        );

        screen.addView(offline);

        setContentView(screen);
    }

    private void showAdmin() {

        LinearLayout screen = baseScreen();

        screen.addView(
                label(
                        "ADMIN",
                        30,
                        Color.rgb(170, 60, 50),
                        true
                )
        );

        screen.addView(
                label(
                        "Sakay Na Administration",
                        19,
                        Color.BLACK,
                        true
                )
        );

        screen.addView(
                label(
                        "Users: 0",
                        17,
                        Color.DKGRAY,
                        false
                )
        );

        screen.addView(
                label(
                        "Drivers: 0",
                        17,
                        Color.DKGRAY,
                        false
                )
        );

        screen.addView(
                label(
                        "Active rides: 0",
                        17,
                        Color.DKGRAY,
                        false
                )
        );

        Button back =
                menuButton(
                        "BACK",
                        Color.GRAY
                );

        back.setOnClickListener(
                view -> showHome()
        );

        screen.addView(back);

        setContentView(screen);
    }
  }
