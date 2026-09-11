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

public class PassengerActivity extends Activity {

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

        TextView subtitle = makeText("Passenger Dashboard", 20);
        subtitle.setGravity(Gravity.CENTER);
        layout.addView(subtitle);

        Button book = makeButton("Book a Ride");
        book.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "Booking screen coming next",
                        Toast.LENGTH_SHORT
                ).show()
        );
        layout.addView(book);

        Button history = makeButton("Ride History");
        history.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "Ride history coming next",
                        Toast.LENGTH_SHORT
                ).show()
        );
        layout.addView(history);

        Button help = makeButton("Help / Emergency");
        help.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "Help and emergency coming next",
                        Toast.LENGTH_SHORT
                ).show()
        );
        layout.addView(help);

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
