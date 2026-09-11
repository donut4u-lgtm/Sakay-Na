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

public class AdminActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showAdminHome();
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

    private void showAdminHome() {

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(25, 25, 25, 25);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = text("SAKAY NA", 30);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.rgb(130, 60, 180));
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        TextView subtitle = text("Admin Dashboard", 20);
        subtitle.setGravity(Gravity.CENTER);
        layout.addView(subtitle);

        Button users = button("👥 Users");
        users.setOnClickListener(v ->
                Toast.makeText(this,
                        "User management coming next",
                        Toast.LENGTH_SHORT).show()
        );
        layout.addView(users);

        Button drivers = button("🛺 Drivers");
        drivers.setOnClickListener(v ->
                Toast.makeText(this,
                        "Driver management coming next",
                        Toast.LENGTH_SHORT).show()
        );
        layout.addView(drivers);

        Button rides = button("🚕 All Rides");
        rides.setOnClickListener(v ->
                Toast.makeText(this,
                        "Ride monitoring coming next",
                        Toast.LENGTH_SHORT).show()
        );
        layout.addView(rides);

        Button reports = button("📊 Reports");
        reports.setOnClickListener(v ->
                Toast.makeText(this,
                        "Reports coming next",
                        Toast.LENGTH_SHORT).show()
        );
        layout.addView(reports);

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
