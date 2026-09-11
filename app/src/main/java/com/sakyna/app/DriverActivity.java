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

        TextView subtitle = makeText("Driver Dashboard", 20);
        subtitle.setGravity(Gravity.CENTER);
        layout.addView(subtitle);

        TextView status = makeText("Status: OFFLINE", 20);
        status.setGravity(Gravity.CENTER);
        layout.addView(status);

        Button onlineButton = makeButton("Go Online");

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

        Button requests = makeButton("Booking Requests");

        requests.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "Booking requests coming next",
                        Toast.LENGTH_SHORT
                ).show()
        );

        layout.addView(requests);

        Button earnings = makeButton("Earnings & History");

        earnings.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "Earnings screen coming next",
                        Toast.LENGTH_SHORT
                ).show()
        );

        layout.addView(earnings);

        Button logout = makeButton("Logout");

        logout.setOnClickListener(v -> logout());

        layout.addView(logout);

        setContentView(layout);
    }

    private void logout() {

        getSharedPreferences("SakayNa", MODE_PRIVATE)
                .edit()
                .remove("current_phone")
                .remove("current_name")
                .remove("current_role")
                .apply();

        Intent intent = new Intent(this, MainActivity.class);

        intent.setFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_NEW_TASK
        );

        startActivity(intent);
        finish();
    }
}
