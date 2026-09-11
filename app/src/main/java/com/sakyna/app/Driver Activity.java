package com.sakyna.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
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

    private TextView text(String value, int size) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(Color.DKGRAY);
        t.setPadding(20, 20, 20, 20);
        return t;
    }

    private Button button(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextSize(17);
        b.setAllCaps(false);
        return b;
    }

    private void showDriverHome() {

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(25, 25, 25, 25);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = text("SAKAY NA", 30);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.rgb(30, 100, 200));
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        TextView subtitle = text("Driver Dashboard", 20);
        subtitle.setGravity(Gravity.CENTER);
        layout.addView(subtitle);

        TextView status = text("Status: OFFLINE", 20);
        status.setGravity(Gravity.CENTER);
        layout.addView(status);

        Button onlineButton = button("🟢 Go Online");

        onlineButton.setOnClickListener(v -> {
            online = !online;

            if (online) {
                status.setText("Status: ONLINE");
                onlineButton.setText("🔴 Go Offline");
                Toast.makeText(this,
                        "You are now accepting rides",
                        Toast.LENGTH_SHORT).show();
            } else {
                status.setText("Status: OFFLINE");
                onlineButton.setText("🟢 Go Online");
                Toast.makeText(this,
                        "You are offline",
                        Toast.LENGTH_SHORT).show();
            }
        });

        layout.addView(onlineButton);

        Button requests = button("📥 Booking Requests");
        requests.setOnClickListener(v ->
                Toast.makeText(this,
                        "Booking requests coming next",
                        Toast.LENGTH_SHORT).show()
        );
        layout.addView(requests);

        Button earnings = button("💰 Earnings & History");
        earnings.setOnClickListener(v ->
                Toast.makeText(this,
                        "Earnings screen coming next",
                        Toast.LENGTH_SHORT).show()
        );
        layout.addView(earnings);

        Button logout = button("Logout");
        logout.setOnClickListener(v -> {
            getSharedPreferences("SakayNa", MODE_PRIVATE)
                    .edit()
                    .remove("current_phone")
                    .remove("current_name")
                    .remove("current_role")
                    .apply();

            android.content.Intent intent =
                    new android.content.Intent(this, MainActivity.class);

            intent.setFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    android.content.Intent.FLAG_ACTIVITY_NEW_TASK);

            startActivity(intent);
            finish();
        });

        layout.addView(logout);

        setContentView(layout);
    }
}
